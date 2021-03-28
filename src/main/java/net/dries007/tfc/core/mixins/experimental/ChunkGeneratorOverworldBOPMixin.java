package net.dries007.tfc.core.mixins.experimental;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorOctaves;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.registries.ForgeRegistry;

import biomesoplenty.common.world.BOPWorldSettings;
import biomesoplenty.common.world.ChunkGeneratorOverworldBOP;
import biomesoplenty.common.world.TerrainSettings;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.api.types.RockCategory;
import net.dries007.tfc.objects.blocks.BlocksTFC;
import net.dries007.tfc.objects.blocks.stone.BlockRockVariant;
import net.dries007.tfc.objects.fluids.FluidsTFC;
import net.dries007.tfc.world.classic.WorldTypeTFC;
import net.dries007.tfc.world.classic.biomes.BiomesTFC;
import net.dries007.tfc.world.classic.genlayers.GenLayerTFC;
import net.dries007.tfc.world.classic.worldgen.experimental.TrackedChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.dries007.tfc.world.classic.WorldTypeTFC.ROCKLAYER2;
import static net.dries007.tfc.world.classic.WorldTypeTFC.ROCKLAYER3;

@Mixin(ChunkGeneratorOverworldBOP.class)
public class ChunkGeneratorOverworldBOPMixin
{
    private static final int yOffset = 114;
    private static final int seaLevel = 32;

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState AIR = Blocks.AIR.getDefaultState();
    private static final IBlockState SALT_WATER = FluidsTFC.SALT_WATER.get().getBlock().getDefaultState();
    private static final IBlockState FRESH_WATER = FluidsTFC.FRESH_WATER.get().getBlock().getDefaultState();
    // private static final IBlockState HOT_WATER = FluidsTFC.HOT_WATER.get().getBlock().getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState BEDROCK = Blocks.BEDROCK.getDefaultState();

    @Shadow(remap = false) private World world;
    @Shadow(remap = false) private Random rand;
    @Shadow(remap = false) private double[] stoneNoiseArray;
    @Shadow(remap = false) private NoiseGeneratorPerlin stoneNoiseGen;
    @Shadow(remap = false) private Map<Biome, TerrainSettings> biomeTerrainSettings;
    @Shadow(remap = false) private BOPWorldSettings settings;
    @Shadow(remap = false) private IBlockState seaBlockState;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void afterCtor(World worldIn, long seed, boolean mapFeaturesEnabled, String chunkProviderSettingsString, CallbackInfo ci)
    {
        this.settings.seaLevel = 96;
        worldIn.setSeaLevel(this.settings.seaLevel);
        this.biomeTerrainSettings.forEach((b, t) -> t.avgHeight = t.avgHeight + (96 - 64));
        this.seaBlockState = SALT_WATER;
    }

    @Redirect(method = "generateChunk", at = @At(value = "NEW", target = "net/minecraft/world/chunk/ChunkPrimer"))
    private ChunkPrimer newPrimer()
    {
        return new TrackedChunkPrimer();
    }

    /**
     * @author BOP Team + Rongmario
     */
    @Overwrite(remap = false)
    public void replaceBlocksForBiome(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes)
    {
        if (!net.minecraftforge.event.ForgeEventFactory.onReplaceBiomeBlocks((IChunkGenerator) this, chunkX, chunkZ, primer, this.world))
        {
            return;
        }
        double d0 = 0.03125D;
        this.stoneNoiseArray = this.stoneNoiseGen.getRegion(this.stoneNoiseArray, chunkX * 16, chunkZ * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        int[] rockLayer1 = GenLayerTFC.initializeRock(world.getSeed() + 1, RockCategory.Layer.TOP, 4).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        int[] rockLayer2 = GenLayerTFC.initializeRock(world.getSeed() + 2, RockCategory.Layer.MIDDLE, 4).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        int[] rockLayer3 = GenLayerTFC.initializeRock(world.getSeed() + 3, RockCategory.Layer.BOTTOM, 4).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        TrackedChunkPrimer trackedPrimer = (TrackedChunkPrimer) primer;
        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                Biome biome = biomes[localZ + localX * 16];
                // biome.genTerrainBlocks(this.world, this.rand, primer, chunkX * 16 + localX, chunkZ * 16 + localZ, this.stoneNoiseArray[localZ + localX * 16]);

                int colIndex = localX << 4 | localZ;

                Rock rock1 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer1[colIndex]);
                Rock rock2 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer2[colIndex]);
                Rock rock3 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer3[colIndex]);

                // trackedPrimer.replaceBlockStates(biome, localX, localZ, rock1, rock2, rock3);
            }
        }
    }

    /**
     * @author Rongmario
     */
    @Overwrite(remap = false)
    public void populate(int chunkX, int chunkZ) {

    }

    /**
     * @author BOP Team + Rongmario
     */
    // @Overwrite(remap = false)
    // public void replaceBlocksForBiome(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes)
    private void kek(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes)
    {
        if (!net.minecraftforge.event.ForgeEventFactory.onReplaceBiomeBlocks((IChunkGenerator) this, chunkX, chunkZ, primer, this.world))
        {
            return;
        }
        double d0 = 0.03125D;
        this.stoneNoiseArray = this.stoneNoiseGen.getRegion(this.stoneNoiseArray, chunkX * 16, chunkZ * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        double[] tfcNoise = new NoiseGeneratorOctaves(rand, 4).generateNoiseOctaves(new double[256], chunkX * 16, chunkZ * 16, 0, 16, 16, 1,d0 * 4.0D, d0, d0 * 4.0D);
        ChunkPrimer outPrimer = new ChunkPrimer();
        boolean[] cliffMap = new boolean[256];
        final Predicate<Biome> isBeach = b -> BiomeDictionary.hasType(b, BiomeDictionary.Type.BEACH);
        final Predicate<Biome> isRiver = b -> BiomeDictionary.hasType(b, BiomeDictionary.Type.RIVER);
        final Predicate<Biome> isOcean = b -> BiomeDictionary.hasType(b, BiomeDictionary.Type.OCEAN);
        int[] rockLayer1 = GenLayerTFC.initializeRock(world.getSeed() + 1, RockCategory.Layer.TOP, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        int[] rockLayer2 = GenLayerTFC.initializeRock(world.getSeed() + 2, RockCategory.Layer.MIDDLE, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        int[] rockLayer3 = GenLayerTFC.initializeRock(world.getSeed() + 3, RockCategory.Layer.BOTTOM, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        int[] seaLevelOffsetMap = new int[256];
        int[] chunkHeightMap = new int[256];
        Arrays.fill(seaLevelOffsetMap, 0);
        Arrays.fill(chunkHeightMap, 0);
        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                Biome biome = biomes[localZ + localX * 16];
                biome.genTerrainBlocks(this.world, this.rand, primer, chunkX * 16 + localX, chunkZ * 16 + localZ, this.stoneNoiseArray[localZ + localX * 16]);

                if (true)
                {
                    continue;
                }

                int colIndex = localZ << 4 | localX;

                Rock rock1 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer1[colIndex]);
                Rock rock2 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer2[colIndex]);
                Rock rock3 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer3[colIndex]);

                int noise = (int) (tfcNoise[colIndex] / 3.0D + 6.0D);
                int smooth = -1;

                IBlockState surfaceBlock = BlockRockVariant.get(rock1, Rock.Type.GRASS).getDefaultState();
                IBlockState subSurfaceBlock = BlockRockVariant.get(rock1, Rock.Type.DIRT).getDefaultState();

                if (!isBeach.test(biome))
                {
                    if (localX != 0 && (isBeach.test(getBiomeOffset(biomes, localZ, localX - 1))))
                    {
                        cliffMap[colIndex] = true;
                    }
                    else if (localZ != 0 && isBeach.test(getBiomeOffset(biomes, localZ - 1, localX)))
                    {
                        cliffMap[colIndex] = true;
                    }
                    else if (isBeach.test(getBiomeOffset(biomes, localZ + 1, localX)) || isBeach.test(getBiomeOffset(biomes, localZ, localX + 1)))
                    {
                        cliffMap[colIndex] = true;
                    }
                }

                //Used to make better rivers
                int nonRiverTiles = 0;
                int nonBeachTiles = 0;
                for (int a = localX - 1; a <= localX + 1; a++)
                {
                    for (int b = localZ - 1; b <= localZ + 1; b++)
                    {
                        Biome offsetBiome = getBiomeOffset(biomes, a, b);
                        if (!isRiver.test(offsetBiome))
                        {
                            nonRiverTiles++;
                        }
                        else if (!isBeach.test(offsetBiome))
                        {
                            nonBeachTiles++;
                        }
                    }
                }
                int highestStone = 0;
                for (int y = 255 - yOffset; y >= 0; y--)
                {
                    /*
                     * HIGH PART (yOffset is used)
                     */
                    outPrimer.setBlockState(localX, y + yOffset, localZ, primer.getBlockState(localX, y, localZ));
                    if (y + 1 < yOffset && outPrimer.getBlockState(localX, y + yOffset, localZ) == AIR)
                    {
                        for (int upCount = 1; BlocksTFC.isSoilOrGravel(outPrimer.getBlockState(localX, y + yOffset + upCount, localZ)); upCount++)
                        {
                            outPrimer.setBlockState(localX, y + yOffset + upCount, localZ, AIR);
                        }
                    }

                    if (outPrimer.getBlockState(localX, y + yOffset, localZ) == STONE)
                    {
                        highestStone = Math.max(highestStone, y);
                    }

                    int highestBeachTheoretical = (highestStone - seaLevel) / 4 + seaLevel;
                    int beachCliffHeight = nonBeachTiles > 0 ? (int) ((highestStone - highestBeachTheoretical) * (nonBeachTiles) / 6.0 + highestBeachTheoretical) : highestBeachTheoretical;

                    //Redo cliffs
                    if (isBeach.test(biome) && y > seaLevel && outPrimer.getBlockState(localX, y + yOffset, localZ) != AIR && y >= beachCliffHeight)
                    {
                        primer.setBlockState(localX, y, localZ, AIR);
                        outPrimer.setBlockState(localX, y + yOffset, localZ, AIR);
                    }
                    //Ensure rivers can't get blocked
                    if (isRiver.test(biome) && y >= seaLevel - 2 && outPrimer.getBlockState(localX, y + yOffset, localZ) != AIR)
                    {
                        if (nonRiverTiles > 0)
                        {
                            if (y >= seaLevel - 1)
                            {
                                primer.setBlockState(localX, y, localZ, y >= seaLevel ? AIR : SALT_WATER);
                                outPrimer.setBlockState(localX, y + yOffset, localZ, y >= seaLevel ? AIR : SALT_WATER);
                            }
                        }
                        else
                        {
                            primer.setBlockState(localX, y, localZ, y >= seaLevel ? AIR : SALT_WATER);
                            outPrimer.setBlockState(localX, y + yOffset, localZ, y >= seaLevel ? AIR : SALT_WATER);
                        }


                        //outPrimer.setBlockState(x, y + yOffset, z, y >= seaLevel ? AIR : SALT_WATER);
                    }
                    else if (!isRiver.test(biome) && nonRiverTiles < 9 && outPrimer.getBlockState(localX, y + yOffset, localZ) == STONE && ((y >= ((highestStone - seaLevel) / (10 - nonRiverTiles) + seaLevel)) || (nonRiverTiles <= 5 && y >= seaLevel)))
                    {
                        primer.setBlockState(localX, y, localZ, y >= seaLevel ? AIR : SALT_WATER);
                        outPrimer.setBlockState(localX, y + yOffset, localZ, y >= seaLevel ? AIR : SALT_WATER);
                    }

                    if (outPrimer.getBlockState(localX, y + yOffset, localZ) == STONE)
                    {
                        if (seaLevelOffsetMap[colIndex] == 0 && y - seaLevel >= 0)
                        {
                            seaLevelOffsetMap[colIndex] = y - seaLevel;
                        }
                        if (chunkHeightMap[colIndex] == 0)
                        {
                            chunkHeightMap[colIndex] = y + yOffset;
                        }
                        if (y + yOffset <= ROCKLAYER3 + seaLevelOffsetMap[colIndex])
                        {
                            outPrimer.setBlockState(localX, y + yOffset, localZ, BlockRockVariant.get(rock3, Rock.Type.RAW).getDefaultState());
                        }
                        else if (y + yOffset <= ROCKLAYER2 + seaLevelOffsetMap[colIndex])
                        {
                            outPrimer.setBlockState(localX, y + yOffset, localZ, BlockRockVariant.get(rock2, Rock.Type.RAW).getDefaultState());
                        }
                        else
                        {
                            outPrimer.setBlockState(localX, y + yOffset, localZ, BlockRockVariant.get(rock1, Rock.Type.RAW).getDefaultState());
                        }
                        // Deserts / dry areas
                        /*
                        if (rainfall < +1.3 * rand.nextGaussian() + 75f)
                        {
                            subSurfaceBlock = surfaceBlock = BlockRockVariant.get(rock1, Rock.Type.RAW).getVariant(Rock.Type.SAND).getDefaultState();
                        }

                         */
                        if (isBeach.test(biome) || isOcean.test(biome))
                        {
                            subSurfaceBlock = surfaceBlock = BlockRockVariant.get(rock1, Rock.Type.SAND).getDefaultState();
                        }
                        /*
                        else if (biome == BiomesTFC.GRAVEL_BEACH)
                        {
                            subSurfaceBlock = surfaceBlock = BlockRockVariant.get(rock1, Rock.Type.GRAVEL).getDefaultState();
                        }

                         */
                        if (smooth == -1)
                        {
                            //The following makes dirt behave nicer and more smoothly, instead of forming sharp cliffs.
                            int arrayIndexx = localX > 0 ? localX - 1 + (localZ * 16) : -1;
                            int arrayIndexX = localX < 15 ? localX + 1 + (localZ * 16) : -1;
                            int arrayIndexz = localZ > 0 ? localX + ((localZ - 1) * 16) : -1;
                            int arrayIndexZ = localZ < 15 ? localX + ((localZ + 1) * 16) : -1;
                            for (int counter = 1; counter < noise / 3; counter++)
                            {
                                if (arrayIndexx >= 0 && seaLevelOffsetMap[colIndex] - (3 * counter) > seaLevelOffsetMap[arrayIndexx] &&
                                    arrayIndexX >= 0 && seaLevelOffsetMap[colIndex] - (3 * counter) > seaLevelOffsetMap[arrayIndexX] &&
                                    arrayIndexz >= 0 && seaLevelOffsetMap[colIndex] - (3 * counter) > seaLevelOffsetMap[arrayIndexz] &&
                                    arrayIndexZ >= 0 && seaLevelOffsetMap[colIndex] - (3 * counter) > seaLevelOffsetMap[arrayIndexZ])
                                {
                                    seaLevelOffsetMap[colIndex]--;
                                    noise--;
                                    y--;
                                }
                            }
                            smooth = (int) (noise * (1d - Math.max(Math.min((y - 16) / 80d, 1), 0)));

                            // Set soil below water
                            for (int c = 1; c < 3; c++)
                            {
                                if (yOffset + y + c > 256) continue;

                                IBlockState current = outPrimer.getBlockState(localX, yOffset + y + c, localZ);
                                if (current != surfaceBlock && current != subSurfaceBlock && !BlocksTFC.isWater(current))
                                {
                                    outPrimer.setBlockState(localX, yOffset + y + c, localZ, AIR);
                                    if (yOffset + y + c + 1 > 256) continue;
                                    if (outPrimer.getBlockState(localX, yOffset + y + c + 1, localZ) == SALT_WATER)
                                        outPrimer.setBlockState(localX, yOffset + y + c, localZ, subSurfaceBlock);
                                }
                            }

                            // Determine the soil depth based on world y
                            int dirtH = Math.max(8 - ((y + yOffset - 24 - WorldTypeTFC.SEALEVEL) / 16), 0);

                            if (smooth > 0)
                            {
                                if (y >= seaLevel - 1 && y + 1 < yOffset && primer.getBlockState(localX, y + 1, localZ) != SALT_WATER && dirtH > 0 && !(isBeach.test(biome) && y > highestBeachTheoretical + 2))
                                {
                                    outPrimer.setBlockState(localX, y + yOffset, localZ, surfaceBlock);

                                    boolean mountains = BiomesTFC.isMountainBiome(biome) || biome == BiomesTFC.HIGH_HILLS || biome == BiomesTFC.HIGH_HILLS_EDGE || biome == BiomesTFC.MOUNTAINS || biome == BiomesTFC.MOUNTAINS_EDGE;
                                    for (int c = 1; c < dirtH && !mountains && !cliffMap[colIndex]; c++)
                                    {
                                        outPrimer.setBlockState(localX, y - c + yOffset, localZ, subSurfaceBlock);
                                        /*
                                        if (c > 1 + (5 - drainage.valueInt))
                                        {
                                            outPrimer.setBlockState(localX, y - c + yOffset, localZ, BlockRockVariant.get(rock1, Rock.Type.GRAVEL).getDefaultState());
                                        }
                                         */
                                    }
                                }
                            }
                        }

                        if (y > seaLevel - 2 && y < seaLevel && primer.getBlockState(localX, y + 1, localZ) == SALT_WATER || y < seaLevel && primer.getBlockState(localX, y + 1, localZ) == SALT_WATER)
                        {
                            if (biome != BiomesTFC.SWAMPLAND) // Most areas have gravel and sand bottoms
                            {
                                if (outPrimer.getBlockState(localX, y + yOffset, localZ) != BlockRockVariant.get(rock1, Rock.Type.SAND).getDefaultState() && rand.nextInt(5) != 0)
                                {
                                    outPrimer.setBlockState(localX, y + yOffset, localZ, BlockRockVariant.get(rock1, Rock.Type.GRAVEL).getDefaultState());
                                }
                            }
                            else // Swamp biomes have bottoms that are mostly dirt
                            {
                                if (outPrimer.getBlockState(localX, y + yOffset, localZ) != BlockRockVariant.get(rock1, Rock.Type.SAND).getDefaultState())
                                {
                                    outPrimer.setBlockState(localX, y + yOffset, localZ, BlockRockVariant.get(rock1, Rock.Type.DIRT).getDefaultState());
                                }
                            }
                        }
                    }
                    //  && biome != BiomesTFC.OCEAN && biome != BiomesTFC.DEEP_OCEAN && biome != BiomesTFC.BEACH && biome != BiomesTFC.GRAVEL_BEACH
                    else if (primer.getBlockState(localX, y, localZ) == SALT_WATER && !(isOcean.test(biome) || isBeach.test(biome)))
                    {
                        outPrimer.setBlockState(localX, y + yOffset, localZ, FRESH_WATER);
                    }
                }

                for (int y = yOffset - 1; y >= 0; y--) // This cannot be optimized with the prev for loop, because the sealeveloffset won't be ready yet.
                {
                    /*
                     * LOW PART (yOffset is NOT used)
                     */
                    /*if (y < 1 + (settings.flatBedrock ? 0 : rand.nextInt(3)))*/ //  + (seaLevelOffsetMap[colIndex] / 3)
                    if (y < 1 + rand.nextInt(5))
                    {
                        outPrimer.setBlockState(localX, y, localZ, BEDROCK);
                    }
                    else if (outPrimer.getBlockState(localX, y, localZ) == AIR)
                    {
                        if (y <= ROCKLAYER3 + seaLevelOffsetMap[colIndex])
                        {
                            outPrimer.setBlockState(localX, y, localZ, BlockRockVariant.get(rock3, Rock.Type.RAW).getDefaultState());
                        }
                        else if (y <= ROCKLAYER2 + seaLevelOffsetMap[colIndex])
                        {
                            outPrimer.setBlockState(localX, y, localZ, BlockRockVariant.get(rock2, Rock.Type.RAW).getDefaultState());
                        }
                        else
                        {
                            outPrimer.setBlockState(localX, y, localZ, BlockRockVariant.get(rock1, Rock.Type.RAW).getDefaultState());
                        }
                        if (BiomesTFC.isBeachBiome(biome) || BiomesTFC.isOceanicBiome(biome))
                        {
                            if (outPrimer.getBlockState(localX, y + 1, localZ) == SALT_WATER)
                            {
                                outPrimer.setBlockState(localX, y, localZ, BlockRockVariant.get(rock1, Rock.Type.SAND).getDefaultState());
                                outPrimer.setBlockState(localX, y - 1, localZ, BlockRockVariant.get(rock1, Rock.Type.SAND).getDefaultState());
                            }
                        }
                    }
                    if (y <= 6 && /*stability.valueInt == 1 && */outPrimer.getBlockState(localX, y, localZ) == AIR)
                    {
                        outPrimer.setBlockState(localX, y, localZ, LAVA);
                        if (outPrimer.getBlockState(localX, y + 1, localZ) != LAVA && rand.nextBoolean())
                        {
                            outPrimer.setBlockState(localX, y + 1, localZ, LAVA);
                        }
                    }
                }
            }
        }
    }

    private Biome getBiomeOffset(Biome[] biomes, int x, int z)
    {
        return biomes[Math.min(255, (z + 1) * 16 + (x + 1))];
    }

}
