package org.survivorsunited.yoinkbombs;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.codec.validation.validator.RangeValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Configuration for Yoink Bomb variants.
 */
public class YoinkBombsConfig {

    private static final double MIN_RADIUS = 0.5D;
    private static final double MAX_RADIUS_DEFAULT = 64.0D;
    private static final double MAX_RADIUS_ORE = 32.0D;
    private static final double MAX_RADIUS_MEGA = 128.0D;
    private static final double MIN_PULL_RADIUS = 1.0D;
    private static final double MAX_PULL_RADIUS = 64.0D;

    public static final BuilderCodec<YoinkBombsConfig> CODEC = buildCodec();

    private static BuilderCodec<YoinkBombsConfig> buildCodec() {
        // Use a raw builder to avoid generic inference issues in chained calls.
        BuilderCodec.Builder builder = (BuilderCodec.Builder) BuilderCodec.builder(YoinkBombsConfig.class, YoinkBombsConfig::new)
                .append(new KeyedCodec("AreaRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setAreaRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).areaRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Radius for Area Bomb block destruction.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("SilkRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setSilkRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).silkRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Radius for Silk Yoink Bomb block destruction.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("MegaYoinkRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setMegaYoinkRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).megaYoinkRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(128.0D), true))
                .documentation("Radius for Mega Yoink Bomb block destruction.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("OreRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setOreRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).oreRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(32.0D), true))
                .documentation("Radius for Ore Bomb connected-ore search.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("HarvesterRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setHarvesterRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).harvesterRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Radius for Harvester Bomb crop harvesting.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("AreaEntityDamageRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setAreaEntityDamageRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).areaEntityDamageRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Entity damage radius for Area Bomb.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("SilkEntityDamageRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setSilkEntityDamageRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).silkEntityDamageRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Entity damage radius for Silk Yoink Bomb.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("MegaYoinkEntityDamageRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setMegaYoinkEntityDamageRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).megaYoinkEntityDamageRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(128.0D), true))
                .documentation("Entity damage radius for Mega Yoink Bomb.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("OreEntityDamageRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setOreEntityDamageRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).oreEntityDamageRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(32.0D), true))
                .documentation("Entity damage radius for Ore Bomb.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("HarvesterEntityDamageRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setHarvesterEntityDamageRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).harvesterEntityDamageRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(0.5D), Double.valueOf(64.0D), true))
                .documentation("Entity damage radius for Harvester Bomb.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("HarvesterWhitelist", (Codec) Codec.STRING), (cfg, value) -> ((YoinkBombsConfig) cfg).setHarvesterWhitelist((String) value), cfg -> ((YoinkBombsConfig) cfg).harvesterWhitelist())
                .documentation("Comma-separated block ID prefixes for Harvester Bombs (e.g. plant_,wood_ for Plant_.* and Wood_.*).")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("BlockWhitelist", (Codec) Codec.STRING), (cfg, value) -> ((YoinkBombsConfig) cfg).setBlockWhitelist((String) value), cfg -> ((YoinkBombsConfig) cfg).blockWhitelist())
                .documentation("Comma-separated block IDs allowed for bomb breaking (first item should be bedrock).")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("YoinkToInventory", (Codec) Codec.BOOLEAN), (cfg, value) -> ((YoinkBombsConfig) cfg).setYoinkToInventory((Boolean) value), cfg -> ((YoinkBombsConfig) cfg).yoinkToInventory())
                .documentation("If true, attempts to add drops directly to inventory before spawning.")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("ItemPullRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setItemPullRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).itemPullRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(1.0D), Double.valueOf(64.0D), true))
                .documentation("Radius within which dropped items are instantly yoinked to the player (when a yoink bomb/arrow is in hand).")
                .add();
        builder = (BuilderCodec.Builder) builder.append(new KeyedCodec("CrossbowPullRadius", (Codec) Codec.DOUBLE), (cfg, value) -> ((YoinkBombsConfig) cfg).setCrossbowPullRadius((Double) value), cfg -> ((YoinkBombsConfig) cfg).crossbowPullRadius())
                .addValidator((Validator) new RangeValidator(Double.valueOf(1.0D), Double.valueOf(64.0D), true))
                .documentation("Radius for fallback item pull when YoinkBombs crossbow is equipped (large-area pull).")
                .add();
        return builder.build();
    }

    private double areaRadius = 4.0D;
    private double silkRadius = 4.0D;
    private double megaYoinkRadius = 8.0D;
    private double oreRadius = 6.0D;
    private double harvesterRadius = 4.0D;
    private double areaEntityDamageRadius = 4.0D;
    private double silkEntityDamageRadius = 4.0D;
    private double megaYoinkEntityDamageRadius = 8.0D;
    private double oreEntityDamageRadius = 3.0D;
    private double harvesterEntityDamageRadius = 4.0D;
    private String harvesterWhitelist = "plant_,wood_";
    private String blockWhitelist = "bedrock,Plant_Wheat,Plant_Carrot,Plant_Potato,Plant_Tomato,Plant_Onion,Plant_Corn,Plant_Beans_Purple,Plant_Rice,Plant_Pumpkin,Plant_Lettuce,Plant_Chilli,Plant_Cauliflower,Crop_Wheat,Crop_Carrot,Crop_Potato,Crop_Tomato,Crop_Onion,Crop_Corn,Crop_Beans,Crop_Rice,Crop_Pumpkin,Crop_Lettuce,Crop_Chilli,Crop_Cauliflower";
    private boolean yoinkToInventory = true;
    private double itemPullRadius = 12.0D;
    private double crossbowPullRadius = 32.0D;

    private void setAreaRadius(double areaRadius) {
        this.areaRadius = areaRadius;
    }

    public double areaRadius() {
        return this.areaRadius;
    }

    private void setSilkRadius(double silkRadius) {
        this.silkRadius = silkRadius;
    }

    public double silkRadius() {
        return this.silkRadius;
    }

    private void setMegaYoinkRadius(double megaYoinkRadius) {
        this.megaYoinkRadius = megaYoinkRadius;
    }

    public double megaYoinkRadius() {
        return this.megaYoinkRadius;
    }

    private void setOreRadius(double oreRadius) {
        this.oreRadius = oreRadius;
    }

    public double oreRadius() {
        return this.oreRadius;
    }

    private void setHarvesterRadius(double harvesterRadius) {
        this.harvesterRadius = harvesterRadius;
    }

    public double harvesterRadius() {
        return this.harvesterRadius;
    }

    private void setHarvesterWhitelist(String harvesterWhitelist) {
        if (harvesterWhitelist != null) {
            this.harvesterWhitelist = harvesterWhitelist;
        }
    }

    public String harvesterWhitelist() {
        return this.harvesterWhitelist;
    }

    private void setAreaEntityDamageRadius(double radius) {
        this.areaEntityDamageRadius = radius;
    }

    public double areaEntityDamageRadius() {
        return this.areaEntityDamageRadius;
    }

    private void setSilkEntityDamageRadius(double radius) {
        this.silkEntityDamageRadius = radius;
    }

    public double silkEntityDamageRadius() {
        return this.silkEntityDamageRadius;
    }

    private void setMegaYoinkEntityDamageRadius(double radius) {
        this.megaYoinkEntityDamageRadius = radius;
    }

    public double megaYoinkEntityDamageRadius() {
        return this.megaYoinkEntityDamageRadius;
    }

    private void setOreEntityDamageRadius(double radius) {
        this.oreEntityDamageRadius = radius;
    }

    public double oreEntityDamageRadius() {
        return this.oreEntityDamageRadius;
    }

    private void setHarvesterEntityDamageRadius(double radius) {
        this.harvesterEntityDamageRadius = radius;
    }

    public double harvesterEntityDamageRadius() {
        return this.harvesterEntityDamageRadius;
    }

    private void setBlockWhitelist(String blockWhitelist) {
        if (blockWhitelist != null) {
            this.blockWhitelist = blockWhitelist;
        }
    }

    public String blockWhitelist() {
        return this.blockWhitelist;
    }

    private void setYoinkToInventory(boolean yoinkToInventory) {
        this.yoinkToInventory = yoinkToInventory;
    }

    public boolean yoinkToInventory() {
        return this.yoinkToInventory;
    }

    private void setItemPullRadius(double itemPullRadius) {
        this.itemPullRadius = itemPullRadius;
    }

    public double itemPullRadius() {
        return this.itemPullRadius;
    }

    private void setCrossbowPullRadius(double crossbowPullRadius) {
        this.crossbowPullRadius = crossbowPullRadius;
    }

    public double crossbowPullRadius() {
        return this.crossbowPullRadius;
    }

    /**
     * Parse the configured block whitelist into normalized entries.
     *
     * @return A list of normalized block IDs, or an empty list if unset.
     */
    public List<String> getBlockWhitelistEntries() {
        if (this.blockWhitelist == null) {
            return Collections.emptyList();
        }
        String trimmed = this.blockWhitelist.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = trimmed.split(",");
        List<String> entries = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String entry = part.trim().toLowerCase();
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Parse the configured harvester whitelist into normalized entries.
     *
     * @return A list of normalized entries, or an empty list if unset.
     */
    public List<String> getHarvesterWhitelistEntries() {
        if (this.harvesterWhitelist == null) {
            return Collections.emptyList();
        }
        String trimmed = this.harvesterWhitelist.trim();
        if (trimmed.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = trimmed.split(",");
        List<String> entries = new ArrayList<>();
        for (String part : parts) {
            if (part == null) {
                continue;
            }
            String entry = part.trim().toLowerCase();
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Check if a block ID is allowed by the whitelist.
     *
     * @param blockId The block ID to test.
     * @return True if allowed or if the whitelist is empty.
     */
    public boolean isBlockWhitelisted(String blockId) {
        if (blockId == null) {
            return false;
        }
        List<String> whitelistEntries = getBlockWhitelistEntries();
        if (whitelistEntries.isEmpty()) {
            return true;
        }
        String normalized = blockId.trim().toLowerCase();
        for (String entry : whitelistEntries) {
            if (normalized.equals(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update a numeric attribute by variant name.
     *
     * @param variant The variant key (area, ore, mega, silk, harvester).
     * @param attr    The attribute key (BlockDamageRadius, EntityDamageRadius).
     * @param value   The new numeric value.
     * @return True if the value was updated.
     */
    public boolean updateAttribute(String variant, String attr, double value) {
        if (variant == null || attr == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return false;
        }
        String variantKey = variant.trim().toLowerCase();
        String attrKey = attr.trim().toLowerCase();
        if (variantKey.isEmpty() || attrKey.isEmpty()) {
            return false;
        }

        boolean entityRadius = "entitydamageradius".equals(attrKey);
        boolean blockRadius = "blockdamageradius".equals(attrKey);
        boolean itemPullRadiusAttr = "itempullradius".equals(attrKey);
        boolean crossbowPullRadiusAttr = "crossbowpullradius".equals(attrKey);

        if ("pull".equals(variantKey)) {
            if (itemPullRadiusAttr) {
                return setItemPullRadiusValue(value);
            }
            if (crossbowPullRadiusAttr) {
                return setCrossbowPullRadiusValue(value);
            }
            return false;
        }

        if (!entityRadius && !blockRadius) {
            return false;
        }

        if ("area".equals(variantKey)) {
            return entityRadius ? setAreaEntityDamageRadiusValue(value) : setAreaRadiusValue(value);
        }
        if ("ore".equals(variantKey)) {
            return entityRadius ? setOreEntityDamageRadiusValue(value) : setOreRadiusValue(value);
        }
        if ("harvester".equals(variantKey)) {
            return entityRadius ? setHarvesterEntityDamageRadiusValue(value) : setHarvesterRadiusValue(value);
        }
        if ("mega".equals(variantKey) || "megayoink".equals(variantKey)) {
            return entityRadius ? setMegaYoinkEntityDamageRadiusValue(value) : setMegaYoinkRadiusValue(value);
        }
        if ("silk".equals(variantKey) || "silkyoink".equals(variantKey)) {
            return entityRadius ? setSilkYoinkEntityDamageRadiusValue(value) : setSilkYoinkRadiusValue(value);
        }
        return false;
    }

    /**
     * Return the max allowed radius for a variant key.
     *
     * @param variant The variant key.
     * @return Max radius, or -1 if invalid.
     */
    public double getMaxRadius(String variant) {
        if (variant == null) {
            return -1D;
        }
        String key = variant.trim().toLowerCase();
        if (key.isEmpty()) {
            return -1D;
        }
        if ("ore".equals(key)) {
            return MAX_RADIUS_ORE;
        }
        if ("mega".equals(key) || "megayoink".equals(key)) {
            return MAX_RADIUS_MEGA;
        }
        if ("area".equals(key) || "silk".equals(key) || "silkyoink".equals(key) || "harvester".equals(key)) {
            return MAX_RADIUS_DEFAULT;
        }
        return -1D;
    }

    /**
     * Return the max allowed value for an attribute.
     *
     * @param variant The variant key.
     * @param attr    The attribute key.
     * @return Max value, or -1 if invalid.
     */
    public double getMaxAttributeValue(String variant, String attr) {
        if (variant == null || attr == null) {
            return -1D;
        }
        String variantKey = variant.trim().toLowerCase();
        String attrKey = attr.trim().toLowerCase();
        if ("pull".equals(variantKey)) {
            if ("itempullradius".equals(attrKey) || "crossbowpullradius".equals(attrKey)) {
                return MAX_PULL_RADIUS;
            }
            return -1D;
        }
        if (!"blockdamageradius".equals(attrKey) && !"entitydamageradius".equals(attrKey)) {
            return -1D;
        }
        return getMaxRadius(variant);
    }

    private boolean setAreaRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.areaRadius = radius;
        return true;
    }

    private boolean setMegaYoinkRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_MEGA) {
            return false;
        }
        this.megaYoinkRadius = radius;
        return true;
    }

    private boolean setOreRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_ORE) {
            return false;
        }
        this.oreRadius = radius;
        return true;
    }

    private boolean setHarvesterRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.harvesterRadius = radius;
        return true;
    }

    private boolean setAreaEntityDamageRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.areaEntityDamageRadius = radius;
        return true;
    }

    private boolean setMegaYoinkEntityDamageRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_MEGA) {
            return false;
        }
        this.megaYoinkEntityDamageRadius = radius;
        return true;
    }

    private boolean setOreEntityDamageRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_ORE) {
            return false;
        }
        this.oreEntityDamageRadius = radius;
        return true;
    }

    private boolean setHarvesterEntityDamageRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.harvesterEntityDamageRadius = radius;
        return true;
    }

    private boolean setSilkYoinkRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.silkRadius = radius;
        return true;
    }

    private boolean setSilkYoinkEntityDamageRadiusValue(double radius) {
        if (radius < MIN_RADIUS || radius > MAX_RADIUS_DEFAULT) {
            return false;
        }
        this.silkEntityDamageRadius = radius;
        return true;
    }

    private boolean setItemPullRadiusValue(double radius) {
        if (radius < MIN_PULL_RADIUS || radius > MAX_PULL_RADIUS) {
            return false;
        }
        this.itemPullRadius = radius;
        return true;
    }

    private boolean setCrossbowPullRadiusValue(double radius) {
        if (radius < MIN_PULL_RADIUS || radius > MAX_PULL_RADIUS) {
            return false;
        }
        this.crossbowPullRadius = radius;
        return true;
    }
}
