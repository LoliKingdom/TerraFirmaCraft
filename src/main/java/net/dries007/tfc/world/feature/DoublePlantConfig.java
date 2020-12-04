package net.dries007.tfc.world.feature;

import net.minecraft.block.BlockState;
import net.minecraft.world.gen.feature.IFeatureConfig;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.dries007.tfc.world.Codecs;

public class DoublePlantConfig implements IFeatureConfig
{
    public static final Codec<DoublePlantConfig> CODEC =  RecordCodecBuilder.create(instance -> instance.group(
        Codecs.LENIENT_BLOCKSTATE.fieldOf("body").forGetter(DoublePlantConfig::getBodyState),
        Codecs.LENIENT_BLOCKSTATE.fieldOf("head").forGetter(DoublePlantConfig::getBodyState)
        ).apply(instance, DoublePlantConfig::new));

    private final BlockState bodyState;
    private final BlockState headState;

    public DoublePlantConfig(BlockState body, BlockState head)
    {
        this.bodyState = body;
        this.headState = head;
    }

    public BlockState getBodyState()
    {
        return bodyState;
    }

    public BlockState getHeadState()
    {
        return headState;
    }
}
