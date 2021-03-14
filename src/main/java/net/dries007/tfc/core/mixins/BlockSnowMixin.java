package net.dries007.tfc.core.mixins;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.util.climate.ITemperatureBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import static net.minecraft.block.BlockSnow.LAYERS;

@Mixin(BlockSnow.class)
public class BlockSnowMixin implements ITemperatureBlock
{
    @Override
    public void onTemperatureUpdateTick(World world, BlockPos pos, IBlockState state)
    {
        if (world.isRaining() && world.getLightFor(EnumSkyBlock.BLOCK, pos.up()) < 11)
        {
            int expectedLayers = -2 - (int) (ClimateTFC.getActualTemp(world, pos) * 0.5f);
            int layers = state.getValue(LAYERS);
            if (expectedLayers > layers && layers < 7) // If we prevent this from getting to a full block, it won't infinitely accumulate
            {
                world.setBlockState(pos, state.withProperty(LAYERS, layers + 1));
            }
        }
    }

    /**
     * @author Rongmario
     * @reason Check our ice types
     */
    @Overwrite
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos)
    {
        IBlockState stateDown = worldIn.getBlockState(pos.down());
        Block block = stateDown.getBlock();

        if (block != Blocks.ICE && block != Blocks.PACKED_ICE && block != Blocks.BARRIER && block != BlocksTFC.SEA_ICE)
        {
            BlockPos downPos = pos.down();
            return stateDown.getBlockFaceShape(worldIn, downPos, EnumFacing.UP) == BlockFaceShape.SOLID || block.isLeaves(stateDown, worldIn, downPos) || block == (Object) this && stateDown.getValue(LAYERS) == 8;
        }
        else
        {
            return false;
        }
    }

    /**
     * @author Rongmario
     * @reason Check actual temp in updateTick
     */
    @Overwrite
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random random)
    {
        if (worldIn.getLightFor(EnumSkyBlock.BLOCK, pos) > 11 || ClimateTFC.getActualTemp(worldIn, pos) > 4f)
        {
            if (state.getValue(LAYERS) > 1)
            {
                worldIn.setBlockState(pos, state.withProperty(LAYERS, state.getValue(LAYERS) - 1));
            }
            else
            {
                worldIn.setBlockToAir(pos);
            }
        }
    }
}
