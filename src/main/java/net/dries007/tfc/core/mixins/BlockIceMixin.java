package net.dries007.tfc.core.mixins;

import net.minecraft.block.BlockIce;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.util.climate.ITemperatureBlock;
import net.dries007.tfc.util.climate.IceMeltHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockIce.class)
public abstract class BlockIceMixin implements ITemperatureBlock
{
    @Shadow protected abstract void turnIntoWater(World worldIn, BlockPos pos);

    @Override
    public void onTemperatureUpdateTick(World world, BlockPos pos, IBlockState state)
    {
        // Either block light (i.e. from torches) or high enough temperature
        if (world.getLightFor(EnumSkyBlock.BLOCK, pos) > 8 || ClimateTFC.getActualTemp(world, pos) > IceMeltHandler.ICE_MELT_THRESHOLD)
        {
            turnIntoWater(world, pos);
        }
    }

}
