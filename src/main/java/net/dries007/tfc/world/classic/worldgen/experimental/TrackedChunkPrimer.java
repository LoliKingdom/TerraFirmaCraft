package net.dries007.tfc.world.classic.worldgen.experimental;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;

import net.minecraftforge.common.BiomeDictionary;

import biomesoplenty.api.block.BOPBlocks;
import biomesoplenty.common.block.BlockBOPDirt;
import biomesoplenty.common.block.BlockBOPGrass;
import biomesoplenty.common.block.BlockBOPMud;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.objects.blocks.stone.BlockRockVariant;
import net.dries007.tfc.objects.fluids.FluidsTFC;

public class TrackedChunkPrimer extends ChunkPrimer
{
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState OVERGROWN_STONE = BOPBlocks.grass.getDefaultState().withProperty(BlockBOPGrass.VARIANT, BlockBOPGrass.BOPGrassType.OVERGROWN_STONE);
    private static final IBlockState DIRT = Blocks.DIRT.getDefaultState();
    private static final IBlockState LOAMY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.LOAMY).withProperty(BlockBOPDirt.COARSE, false);
    private static final IBlockState SILTY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.SILTY).withProperty(BlockBOPDirt.COARSE, false);
    private static final IBlockState SANDY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.SANDY).withProperty(BlockBOPDirt.COARSE, false);
    private static final IBlockState COARSE_LOAMY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.LOAMY).withProperty(BlockBOPDirt.COARSE, true);
    private static final IBlockState COARSE_SILTY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.SILTY).withProperty(BlockBOPDirt.COARSE, true);
    private static final IBlockState COARSE_SANDY_DIRT = BOPBlocks.dirt.getDefaultState().withProperty(BlockBOPDirt.VARIANT, BlockBOPDirt.BOPDirtType.SANDY).withProperty(BlockBOPDirt.COARSE, true);
    private static final IBlockState GRASS = Blocks.GRASS.getDefaultState();
    private static final IBlockState LOAMY_GRASS = BOPBlocks.grass.getDefaultState().withProperty(BlockBOPGrass.VARIANT, BlockBOPGrass.BOPGrassType.LOAMY);
    private static final IBlockState SILTY_GRASS = BOPBlocks.grass.getDefaultState().withProperty(BlockBOPGrass.VARIANT, BlockBOPGrass.BOPGrassType.SILTY);
    private static final IBlockState SANDY_GRASS = BOPBlocks.grass.getDefaultState().withProperty(BlockBOPGrass.VARIANT, BlockBOPGrass.BOPGrassType.SANDY);
    private static final IBlockState GRAVEL = Blocks.GRAVEL.getDefaultState();
    private static final IBlockState SAND = Blocks.SAND.getDefaultState();
    private static final IBlockState MUD = BOPBlocks.mud.getDefaultState().withProperty(BlockBOPMud.VARIANT, BlockBOPMud.MudType.MUD); // Why...?
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    private static final IBlockState SALT_WATER = FluidsTFC.SALT_WATER.get().getBlock().getDefaultState();

    // Air: -1, Stone: 1, Dirt: 2, Grass Block: 3, Gravel: 4, Sand: 5, Water: 6, Clay Grass: 7, Clay Dirt: 8, Dry Grass: 9, Peat: 10, everything else: 0
    private final byte[] trackingArray = new byte[65536];
    private final int[] peaks = new int[256];

    public void replaceBlockStates(Biome biome, int x, int z, Rock rock1, Rock rock2, Rock rock3)
    {
        int topY = peaks[x << 4 | z];
        int cutoff = topY / 3;
        int cutOff1 = topY - cutoff;
        int cutOff2 = topY - (cutoff * 2);

        for (int localY = topY; localY > 0; --localY)
        {
            Rock selectedRock = rock1;
            if (cutOff2 >= localY)
            {
                selectedRock = rock3;
            }
            else if (cutOff1 >= localY)
            {
                selectedRock = rock2;
            }
            switch (trackingArray[x << 12 | z << 8 | localY])
            {
                case 1:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.RAW).getDefaultState());
                    break;
                case 2:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.DIRT).getDefaultState());
                    break;
                case 3:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.GRASS).getDefaultState());
                    break;
                case 4:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.GRAVEL).getDefaultState());
                    break;
                case 5:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.SAND).getDefaultState());
                    break;
                case 6:
                    if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN))
                    {
                        super.setBlockState(x, localY, z, SALT_WATER);
                    }
                    break;
                case 7:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.CLAY_GRASS).getDefaultState());
                    break;
                case 8:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.CLAY).getDefaultState());
                    break;
                case 9:
                    super.setBlockState(x, localY, z, BlockRockVariant.get(selectedRock, Rock.Type.DRY_GRASS).getDefaultState());
                    break;
                case 10:
                    super.setBlockState(x, localY, z, trackingArray[x << 12 | z << 8 | (localY + 1)] == -1 ? BlocksTFC.PEAT_GRASS.getDefaultState() : BlocksTFC.PEAT.getDefaultState());
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void setBlockState(int x, int y, int z, IBlockState state)
    {
        if (state == AIR)
        {
            trackingArray[x << 12 | z << 8 | y] = -1;
        }
        else if (state == STONE || state == OVERGROWN_STONE)
        {
            trackingArray[x << 12 | z << 8 | y] = 1;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == DIRT)
        {
            trackingArray[x << 12 | z << 8 | y] = 2;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == GRASS)
        {
            trackingArray[x << 12 | z << 8 | y] = 3;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == GRAVEL || state == COARSE_LOAMY_DIRT || state == COARSE_SILTY_DIRT || state == COARSE_SANDY_DIRT)
        {
            trackingArray[x << 12 | z << 8 | y] = 4;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == SAND || state == SILTY_DIRT || state == SANDY_DIRT)
        {
            trackingArray[x << 12 | z << 8 | y] = 5;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == WATER)
        {
            trackingArray[x << 12 | z << 8 | y] = 6;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == LOAMY_GRASS)
        {
            trackingArray[x << 12 | z << 8 | y] = 7;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == LOAMY_DIRT)
        {
            trackingArray[x << 12 | z << 8 | y] = 8;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == SILTY_GRASS || state == SANDY_GRASS)
        {
            trackingArray[x << 12 | z << 8 | y] = 9;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else if (state == MUD)
        {
            trackingArray[x << 12 | z << 8 | y] = 10;

            if (peaks[x << 4 | z] < y)
            {
                peaks[x << 4 | z] = y;
            }
        }
        else
        {
            trackingArray[x << 12 | z << 8 | y] = 0;
        }
        super.setBlockState(x, y, z, state);
    }

    public int getHighestPoint(int x, int z)
    {
        return peaks[x << 4 | z];
    }
}
