package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Entry point for the Yoink Bombs plugin.
 */
public class YoinkBombsPlugin extends JavaPlugin {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final Config<YoinkBombsConfig> config;
    private Object regionService;
    private final List<PendingYoink> pendingYoinks = new CopyOnWriteArrayList<>();

    public YoinkBombsPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        this.config = withConfig(YoinkBombsConfig.CODEC);
        LOGGER.atInfo().log("Hello from " + this.getName() + " version " + this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        LOGGER.atInfo().log("Setting up plugin " + this.getName());
        this.getCommandRegistry().registerCommand(new YoinkBombsCommand(this.config));
        this.config.save();
        YoinkBombsExplosionListener explosionListener = new YoinkBombsExplosionListener(this.config, this.pendingYoinks);
        registerEntityRemoveListener(explosionListener);
    }

    private void registerEntityRemoveListener(YoinkBombsExplosionListener listener) {
        try {
            Class<?> eventClass = Class.forName("com.hypixel.hytale.server.core.event.events.entity.EntityRemoveEvent");
            this.getEventRegistry().registerGlobal(eventClass, listener::onEntityRemove);
        } catch (ClassNotFoundException e) {
            LOGGER.atInfo().log("EntityRemoveEvent not found; yoink at explosion position may not trigger.");
        }
    }

    @Override
    protected void start() {
        super.start();
        this.regionService = resolveRegionService();
        if (this.regionService != null) {
            LOGGER.atInfo().log("WorldProtect detected. Protection checks enabled.");
        } else {
            LOGGER.atInfo().log("WorldProtect not detected. Protection checks disabled.");
        }
        this.getEntityStoreRegistry().registerSystem(new YoinkBombsSystem(this.config, this.regionService));
        this.getEntityStoreRegistry().registerSystem(new YoinkBombsItemPullSystem(this.config, this.pendingYoinks));
    }

    public Object getRegionService() {
        return this.regionService;
    }

    private Object resolveRegionService() {
        try {
            Class<?> mapAccess = Class.forName("dev.worldprotect.worldprotect.map.WorldProtectMapAccess");
            java.lang.reflect.Field field = mapAccess.getDeclaredField("service");
            field.setAccessible(true);
            return field.get(null);
        } catch (Exception ignored) {
        }
        return null;
    }
}
