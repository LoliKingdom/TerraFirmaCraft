package net.dries007.tfc.core.mixins;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockStaticLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.util.climate.ITemperatureBlock;
import net.dries007.tfc.util.climate.IceMeltHandler;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockStaticLiquid.class)
public abstract class BlockStaticLiquidMixin extends BlockLiquid implements ITemperatureBlock
{
    protected BlockStaticLiquidMixin(Material materialIn)
    {
        super(materialIn);
    }

    @Override
    public void onTemperatureUpdateTick(World world, BlockPos pos, IBlockState state)
    {
        if (getStaticBlock(Material.WATER) != state.getBlock())
        {
            return;
        }
        if (state.getValue(LEVEL) == 0 && world.getLightFor(EnumSkyBlock.BLOCK, pos) < 10 && ClimateTFC.getActualTemp(world, pos) < IceMeltHandler.WATER_FREEZE_THRESHOLD)
        {
            for (EnumFacing face : EnumFacing.HORIZONTALS)
            {
                if (world.getBlockState(pos.offset(face)).getBlock() != this)
                {
                    world.setBlockState(pos, Blocks.ICE.getDefaultState());
                    break;
                }
            }
        }
    }
}
