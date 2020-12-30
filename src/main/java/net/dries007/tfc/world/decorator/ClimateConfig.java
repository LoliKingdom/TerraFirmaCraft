package net.dries007.tfc.world.decorator;

import java.util.Random;

import net.minecraft.world.gen.placement.IPlacementConfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dries007.tfc.world.chunkdata.ForestType;

public class ClimateConfig implements IPlacementConfig
{
    public static final Codec<ClimateConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.FLOAT.optionalFieldOf("min_temperature", -Float.MAX_VALUE).forGetter(c -> c.minTemp),
        Codec.FLOAT.optionalFieldOf("max_temperature", Float.MAX_VALUE).forGetter(c -> c.maxTemp),
        Codec.FLOAT.optionalFieldOf("min_rainfall", -Float.MAX_VALUE).forGetter(c -> c.minRainfall),
        Codec.FLOAT.optionalFieldOf("max_rainfall", Float.MAX_VALUE).forGetter(c -> c.maxRainfall),
        ForestType.CODEC.optionalFieldOf("min_forest", ForestType.NONE).forGetter(c -> c.minForest),
        ForestType.CODEC.optionalFieldOf("max_forest", ForestType.OLD_GROWTH).forGetter(c -> c.maxForest),
        Codec.BOOL.optionalFieldOf("fuzzy", false).forGetter(c -> c.fuzzy)
    ).apply(instance, ClimateConfig::new));

    private final float minTemp;
    private final float maxTemp;
    private final float targetTemp;
    private final float minRainfall;
    private final float maxRainfall;
    private final float targetRainfall;
    private final ForestType minForest;
    private final ForestType maxForest;
    private final boolean fuzzy;

    public ClimateConfig(float minTemp, float maxTemp, float minRainfall, float maxRainfall, ForestType minForest, ForestType maxForest, boolean fuzzy)
    {
        this.minTemp = minTemp;
        this.maxTemp = maxTemp;
        this.targetTemp = (minTemp + maxTemp) / 2f;
        this.minRainfall = minRainfall;
        this.maxRainfall = maxRainfall;
        this.targetRainfall = (minRainfall + maxRainfall) / 2f;
        this.minForest = minForest;
        this.maxForest = maxForest;
        this.fuzzy = fuzzy;
    }

    public boolean isValid(float temperature, float rainfall, ForestType forestType, Random random)
    {
        if(minTemp <= temperature && temperature <= maxTemp && minRainfall <= rainfall && rainfall <= maxRainfall && minForest.ordinal() <= forestType.ordinal() && forestType.ordinal() <= maxForest.ordinal())
        {
            if (fuzzy)
            {
                float normTempDelta = Math.abs(temperature - targetTemp) / (maxTemp - minTemp);
                float normRainfallDelta = Math.abs(rainfall - targetRainfall) / (maxRainfall - minRainfall);
                return random.nextFloat() * random.nextFloat() > Math.max(normTempDelta, normRainfallDelta);
            }
            return true;
        }
        return false;
    }
}
