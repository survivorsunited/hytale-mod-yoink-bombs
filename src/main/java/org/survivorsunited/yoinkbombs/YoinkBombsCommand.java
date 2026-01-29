package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;

/**
 * Command for updating Yoink Bomb configuration values.
 */
public class YoinkBombsCommand extends CommandBase {

    private final OptionalArg<String> variantArg;
    private final OptionalArg<String> attrArg;
    private final OptionalArg<Double> numberArg;
    private final Config<YoinkBombsConfig> config;

    /**
     * Create the command with required arguments.
     *
     * @param config The config wrapper for reading/writing values.
     */
    public YoinkBombsCommand(@Nonnull Config<YoinkBombsConfig> config) {
        super("yoinkbombs", "Update Yoink Bomb settings.");
        this.setPermissionGroup(GameMode.Adventure);
        this.requirePermission("yoinkbombs.admin");
        this.config = config;
        this.variantArg = this.withOptionalArg("variant", "Bomb variant (area, ore, harvester, mega, silk) or pull", ArgTypes.STRING);
        this.attrArg = this.withOptionalArg("attr", "Attribute (BlockDamageRadius, EntityDamageRadius, ItemPullRadius, CrossbowPullRadius)", ArgTypes.STRING);
        this.numberArg = this.withOptionalArg("number", "New numeric value", ArgTypes.DOUBLE);
    }

    @Override
    protected void executeSync(@Nonnull CommandContext ctx) {
        if (!ctx.provided(this.variantArg) || !ctx.provided(this.attrArg) || !ctx.provided(this.numberArg)) {
            // Handle the standalone reload subcommand.
            if (ctx.provided(this.variantArg) && !ctx.provided(this.attrArg) && !ctx.provided(this.numberArg)) {
                String variantOnly = ctx.get(this.variantArg);
                if (variantOnly != null && "reload".equalsIgnoreCase(variantOnly.trim())) {
                    if (!this.hasPermission(ctx.sender())) {
                        ctx.sendMessage(Message.raw("You must be OP to update Yoink Bomb settings."));
                        return;
                    }
                    if (attemptReload()) {
                        ctx.sendMessage(Message.raw("Yoink Bombs config reloaded from disk."));
                    } else {
                        ctx.sendMessage(Message.raw("Config reload is not supported by this server build."));
                    }
                    return;
                }
            }
            ctx.sendMessage(Message.raw("Available: area, ore, harvester, mega, silk, pull."));
            ctx.sendMessage(Message.raw("Usage: /yoinkbombs <variant> <attr> <number>"));
            ctx.sendMessage(Message.raw("  Bomb attrs: BlockDamageRadius, EntityDamageRadius. Pull attrs: ItemPullRadius, CrossbowPullRadius (1-64)."));
            ctx.sendMessage(Message.raw("Usage: /yoinkbombs reload"));
            return;
        }

        if (!this.hasPermission(ctx.sender())) {
            ctx.sendMessage(Message.raw("You must be OP to update Yoink Bomb settings."));
            return;
        }

        String variant = ctx.get(this.variantArg);
        String attr = ctx.get(this.attrArg);
        Double number = ctx.get(this.numberArg);
        YoinkBombsConfig cfg = this.config.get();
        double maxValue = cfg.getMaxAttributeValue(variant, attr);
        if (maxValue <= 0D) {
            ctx.sendMessage(Message.raw("Unknown variant or attr. Use: area, ore, harvester, mega, silk, or pull with ItemPullRadius/CrossbowPullRadius."));
            return;
        }

        double minVal = "pull".equalsIgnoreCase(variant.trim()) ? 1.0 : 0.5;
        if (!cfg.updateAttribute(variant, attr, number)) {
            ctx.sendMessage(Message.raw(String.format("Value must be between %s and %s for %s %s.", minVal, maxValue, variant, attr)));
            return;
        }

        this.config.save();
        ctx.sendMessage(Message.raw(String.format("Updated %s %s to %s.", variant, attr, number)));
    }

    /**
     * Attempt to reload the config from disk using reflection.
     *
     * @return True if a reload method was invoked successfully.
     */
    private boolean attemptReload() {
        // Use reflection to avoid compile-time dependency on a specific API method.
        String[] candidateMethods = new String[]{"reload", "load", "read", "refresh"};
        for (String methodName : candidateMethods) {
            try {
                java.lang.reflect.Method method = this.config.getClass().getMethod(methodName);
                Object result = method.invoke(this.config);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
                return true;
            } catch (NoSuchMethodException ignored) {
                // Try the next possible reload method name.
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }
}
