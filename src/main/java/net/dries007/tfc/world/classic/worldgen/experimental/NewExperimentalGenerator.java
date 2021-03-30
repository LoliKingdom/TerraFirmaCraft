package net.dries007.tfc.world.classic.worldgen.experimental;

import java.util.*;
import javax.annotation.Nullable;

import net.minecraft.block.BlockFalling;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.WorldGenDungeons;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.*;

import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.TerrainGen;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;

import biomesoplenty.api.biome.BOPBiomes;
import biomesoplenty.common.biome.overworld.BOPOverworldBiome;
import biomesoplenty.common.world.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.api.types.RockCategory;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;
import net.dries007.tfc.world.classic.genlayers.GenLayerTFC;
import net.dries007.tfc.world.classic.worldgen.WorldGenLargeRocks;
import net.dries007.tfc.world.classic.worldgen.WorldGenOreVeins;

import static net.minecraftforge.event.terraingen.InitMapGenEvent.EventType.*;
import static net.minecraftforge.event.terraingen.PopulateChunkEvent.Populate.EventType.*;

public class NewExperimentalGenerator implements IChunkGenerator
{
    private static final int yOffset = 114;
    private static final int seaLevel = 32;

    private static final IBlockState STONE = Blocks.STONE.getDefaultState();
    private static final IBlockState WATER = Blocks.WATER.getDefaultState();
    // private static final IBlockState HOT_WATER = FluidsTFC.HOT_WATER.get().getBlock().getDefaultState();
    private static final IBlockState LAVA = Blocks.LAVA.getDefaultState();
    private static final IBlockState BEDROCK = Blocks.BEDROCK.getDefaultState();

    private static final float[] radialFalloff5x5 = new float[25];
    private static final float[] radialStrongFalloff5x5 = new float[25];

    static {
        for (int j = -2; j <= 2; ++j)
        {
            for (int k = -2; k <= 2; ++k)
            {
                radialFalloff5x5[j + 2 + (k + 2) * 5] = 0.06476162171F / MathHelper.sqrt((float) (j * j + k * k) + 0.2F);
                radialStrongFalloff5x5[j + 2 + (k + 2) * 5] = 0.076160519601F / ((float) (j * j + k * k) + 0.2F);
            }
        }
    }

    private final World world;
    private final Random rand;
    private final BOPWorldSettings worldSettings;
    private final NoiseGeneratorOctaves xyzNoiseGenA, xyzNoiseGenB, xyzBalanceNoiseGen;
    private final NoiseGeneratorPerlin stoneNoiseGen;
    private final NoiseGeneratorBOPByte byteNoiseGen;

    private final MapGenBase caveGenerator;
    private final MapGenVillage villageGenerator;
    private final MapGenMineshaft mineshaftGenerator;
    private final MapGenScatteredFeature scatteredFeatureGenerator;
    private final MapGenBase ravineGenerator;
    private final StructureOceanMonument oceanMonumentGenerator;
    private final WoodlandMansion woodlandMansionGenerator;

    private final double[] stoneNoiseArray;
    private final double[] noiseArray;
    private final Map<Biome, TerrainSettings> biomeTerrainSettings;

    private double[] xyzBalanceNoiseArray, xyzNoiseArrayA, xyzNoiseArrayB;

    private int[] rockLayer1, rockLayer2, rockLayer3;

    public NewExperimentalGenerator(World world, String settingsString)
    {
        this.world = world;
        this.rand = new Random(world.getSeed());
        this.worldSettings = new BOPWorldSettings();
        worldSettings.seaLevel = 96;
        worldSettings.amplitude = 2.4F;
        worldSettings.biomeSize = BOPWorldSettings.BiomeSize.SMALL;
        worldSettings.generateBopGems = false;
        worldSettings.generateNetherHives = false;
        worldSettings.dungeonChance = 4;
        // this.worldSettings = new BOPWorldSettings(settingsString);
        this.xyzNoiseGenA = new NoiseGeneratorOctaves(rand, 16);
        this.xyzNoiseGenB = new NoiseGeneratorOctaves(rand, 16);
        this.xyzBalanceNoiseGen = new NoiseGeneratorOctaves(rand, 8);
        this.stoneNoiseGen = new NoiseGeneratorPerlin(rand, 4);
        this.byteNoiseGen = new NoiseGeneratorBOPByte(rand, 6, 5, 5);
        this.stoneNoiseArray = new double[256];
        this.noiseArray = new double[825];

        this.caveGenerator = TerrainGen.getModdedMapGen(new MapGenCaves(), CAVE);
        this.villageGenerator = (MapGenVillage) TerrainGen.getModdedMapGen(new MapGenVillage(), VILLAGE);
        this.mineshaftGenerator = (MapGenMineshaft) TerrainGen.getModdedMapGen(new MapGenMineshaft(), MINESHAFT);
        this.scatteredFeatureGenerator = (MapGenScatteredFeature) TerrainGen.getModdedMapGen(new BOPMapGenScatteredFeature(), SCATTERED_FEATURE);
        this.ravineGenerator = TerrainGen.getModdedMapGen(new MapGenRavine(), RAVINE);
        this.oceanMonumentGenerator = (StructureOceanMonument) TerrainGen.getModdedMapGen(new StructureOceanMonument(), OCEAN_MONUMENT);

        // TODO: Remove and adopt Forge's setup whenever that is added
        this.woodlandMansionGenerator = new WoodlandMansion(new FakeMansionChunkProvider());
        this.biomeTerrainSettings = new Object2ObjectOpenHashMap<>();
        world.setSeaLevel(worldSettings.seaLevel);
        for (Biome biome : ForgeRegistries.BIOMES)
        {
            TerrainSettings setting = biome instanceof BOPOverworldBiome ? ((BOPOverworldBiome) biome).terrainSettings : TerrainSettings.forVanillaBiome(biome);
            setting.avgHeight += 96 - 64;
            this.biomeTerrainSettings.put(biome, setting);
        }
    }

    @Override
    public Chunk generateChunk(int chunkX, int chunkZ)
    {
        rand.setSeed((long) chunkX * 341873128712L + (long) chunkZ * 132897987541L);
        TrackedChunkPrimer chunkprimer = new TrackedChunkPrimer();
        prepareChunk(chunkX, chunkZ, chunkprimer);
        Biome[] biomes = world.getBiomeProvider().getBiomes(null, chunkX * 16, chunkZ * 16, 16, 16);
        generateTerrain(chunkX, chunkZ, chunkprimer, biomes);

        // No stronghold generation, that would be a custom structure.
        caveGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        ravineGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        mineshaftGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        villageGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        scatteredFeatureGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        oceanMonumentGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        woodlandMansionGenerator.generate(world, chunkX, chunkZ, chunkprimer);
        Chunk chunk = new Chunk(world, chunkprimer, chunkX, chunkZ);
        ChunkDataTFC chunkData = chunk.getCapability(ChunkDataProvider.CHUNK_DATA_CAPABILITY, null);
        if (chunkData == null)
        {
            throw new IllegalStateException("Chunk Data not found!");
        }
        chunkData.setGenerationData(rockLayer1, rockLayer2, rockLayer3, 0F, 0F, 0F, 0F, 0F);
        byte[] chunkBiomes = chunk.getBiomeArray();
        for (int k = 0; k < chunkBiomes.length; ++k)
        {
            chunkBiomes[k] = (byte) Biome.getIdForBiome(biomes[k]);
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int chunkX, int chunkZ)
    {
        boolean prevLogging = ForgeModContainer.logCascadingWorldGeneration;
        ForgeModContainer.logCascadingWorldGeneration = false;
        BlockFalling.fallInstantly = true;

        final int x = chunkX << 4;
        final int z = chunkZ << 4;
        BlockPos blockpos = new BlockPos(x, 0, z);

        Biome biome = world.getBiome(blockpos.add(16, 0, 16));

        rand.setSeed(world.getSeed());
        long l0 = rand.nextLong() / 2L * 2L + 1L;
        long l1 = rand.nextLong() / 2L * 2L + 1L;
        rand.setSeed((long) chunkX * l0 + (long) chunkZ * l1 ^ world.getSeed());
        boolean hasVillageGenerated = false;
        ChunkPos chunkPos = new ChunkPos(chunkX, chunkZ);

        ForgeEventFactory.onChunkPopulate(true, this, world, rand, chunkX, chunkZ, hasVillageGenerated);

        new WorldGenOreVeins().generate(rand, chunkX, chunkZ, world, this, world.getChunkProvider());

        mineshaftGenerator.generateStructure(world, rand, chunkPos);
        hasVillageGenerated = villageGenerator.generateStructure(world, rand, chunkPos);
        scatteredFeatureGenerator.generateStructure(world, rand, chunkPos);
        oceanMonumentGenerator.generateStructure(world, rand, chunkPos);
        woodlandMansionGenerator.generateStructure(world, rand, chunkPos);

        BlockPos decorateStart = blockpos.add(8, 0, 8);
        BlockPos target;

        if (biome.getRainfall() > 0.01F && biome != Biomes.DESERT && biome != Biomes.DESERT_HILLS && worldSettings.useWaterLakes && !hasVillageGenerated && this.rand.nextInt(worldSettings.waterLakeChance) == 0 && TerrainGen.populate(this, world, rand, chunkX, chunkZ, hasVillageGenerated, LAKE))
        {
            target = decorateStart.add(rand.nextInt(16), rand.nextInt(256), rand.nextInt(16));
            new WorldGenLakes(Blocks.WATER).generate(world, rand, target);
        }
        if (!hasVillageGenerated && (BOPBiomes.redwood_forest.isPresent() && biome != BOPBiomes.redwood_forest.get()) && (BOPBiomes.redwood_forest_edge.isPresent() && biome != BOPBiomes.redwood_forest_edge.get()) && (BOPBiomes.wasteland.isPresent() && biome != BOPBiomes.wasteland.get()) && this.rand.nextInt(worldSettings.lavaLakeChance / 10) == 0 && worldSettings.useLavaLakes && TerrainGen.populate(this, world, rand, chunkX, chunkZ, hasVillageGenerated, PopulateChunkEvent.Populate.EventType.LAVA))
        {
            target = decorateStart.add(rand.nextInt(16), rand.nextInt(248) + 8, rand.nextInt(16));
            if (target.getY() < 63 || this.rand.nextInt(worldSettings.lavaLakeChance / 8) == 0)
            {
                new WorldGenLakes(Blocks.LAVA).generate(world, rand, target);
            }
        }
        if (worldSettings.useDungeons && TerrainGen.populate(this, world, rand, chunkX, chunkZ, hasVillageGenerated, DUNGEON))
        {
            for (int i = 0; i < worldSettings.dungeonChance; ++i)
            {
                target = decorateStart.add(rand.nextInt(16), rand.nextInt(256), rand.nextInt(16));
                new WorldGenDungeons().generate(world, rand, target);
            }
        }
        biome.decorate(world, rand, new BlockPos(x, 0, z));
        if (TerrainGen.populate(this, world, rand, chunkX, chunkZ, hasVillageGenerated, ANIMALS))
        {
            WorldEntitySpawner.performWorldGenSpawning(world, biome, x + 8, z + 8, 16, 16, rand); // TODO
        }
        if (TerrainGen.populate(this, world, rand, chunkX, chunkZ, hasVillageGenerated, ICE))
        {
            for (int i = 0; i < 16; ++i)
            {
                for (int j = 0; j < 16; ++j)
                {
                    target = world.getPrecipitationHeight(decorateStart.add(i, 0, j));
                    biome = world.getBiome(target);
                    // if it's cold enough for ice, and there's exposed water, then freeze it
                    BlockPos belowTarget = target.down();
                    if (world.canBlockFreezeWater(belowTarget))
                    {
                        world.setBlockState(belowTarget, Blocks.ICE.getDefaultState(), 2);
                    }
                    // if it's cold enough for snow, add a layer of snow
                    if (biome.getRainfall() > 0.01F && world.canSnowAt(target, true))
                    {
                        world.setBlockState(target, Blocks.SNOW_LAYER.getDefaultState(), 2);
                    }
                }
            }
        }

        if (rand.nextFloat() > 0.9)
        {
            new WorldGenLargeRocks().generate(rand, chunkX, chunkZ, world, this, world.getChunkProvider());
        }

        ForgeEventFactory.onChunkPopulate(false, this, world, rand, chunkX, chunkZ, hasVillageGenerated);

        BlockFalling.fallInstantly = false;
        ForgeModContainer.logCascadingWorldGeneration = prevLogging;
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z)
    {
        if (chunkIn.getInhabitedTime() < 3600L)
        {
            return this.oceanMonumentGenerator.generateStructure(world, rand, new ChunkPos(x, z));
        }
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos)
    {
        Biome biome = this.world.getBiome(pos);
        if (creatureType == EnumCreatureType.MONSTER && this.scatteredFeatureGenerator.isSwampHut(pos))
        {
            return this.scatteredFeatureGenerator.getMonsters();
        }
        if (creatureType == EnumCreatureType.MONSTER && this.oceanMonumentGenerator.isPositionInStructure(this.world, pos))
        {
            return this.oceanMonumentGenerator.getMonsters();
        }
        return biome.getSpawnableList(creatureType);
    }

    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos pos, boolean findUnexplored)
    {
        if ("Mansion".equals(structureName))
        {
            return woodlandMansionGenerator.getNearestStructurePos(worldIn, pos, findUnexplored);
        }
        else if ("Monument".equals(structureName))
        {
            return oceanMonumentGenerator.getNearestStructurePos(worldIn, pos, findUnexplored);
        }
        else if ("Village".equals(structureName))
        {
            return this.villageGenerator.getNearestStructurePos(worldIn, pos, findUnexplored);
        }
        else if ("Mineshaft".equals(structureName))
        {
            return this.mineshaftGenerator.getNearestStructurePos(worldIn, pos, findUnexplored);
        }
        else
        {
            return "Temple".equals(structureName) ? this.scatteredFeatureGenerator.getNearestStructurePos(worldIn, pos, findUnexplored) : null;
        }
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int chunkX, int chunkZ)
    {
        this.mineshaftGenerator.generate(world, chunkX, chunkZ, null);
        this.villageGenerator.generate(world, chunkX, chunkZ, null);
        this.scatteredFeatureGenerator.generate(world, chunkX, chunkZ, null);
        this.oceanMonumentGenerator.generate(world, chunkX, chunkZ, null);
        this.woodlandMansionGenerator.generate(world, chunkX, chunkZ, null);
    }

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos)
    {
        if ("Mansion".equals(structureName))
        {
            return this.woodlandMansionGenerator.isInsideStructure(pos);
        }
        else if ("Monument".equals(structureName))
        {
            return this.oceanMonumentGenerator.isInsideStructure(pos);
        }
        else if ("Village".equals(structureName))
        {
            return this.villageGenerator.isInsideStructure(pos);
        }
        else if ("Mineshaft".equals(structureName))
        {
            return this.mineshaftGenerator.isInsideStructure(pos);
        }
        else
        {
            return ("Temple".equals(structureName) && this.scatteredFeatureGenerator != null) && this.scatteredFeatureGenerator.isInsideStructure(pos);
        }
    }

    private void generateTerrain(int chunkX, int chunkZ, TrackedChunkPrimer primer, Biome[] biomes)
    {
        if (!ForgeEventFactory.onReplaceBiomeBlocks(this, chunkX, chunkZ, primer, world))
        {
            return;
        }
        double d0 = 0.03125D;
        stoneNoiseGen.getRegion(stoneNoiseArray, chunkX * 16, chunkZ * 16, 16, 16, d0 * 2.0D, d0 * 2.0D, 1.0D);
        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                Biome biome = biomes[localZ + localX * 16];
                biome.genTerrainBlocks(world, rand, primer, chunkX * 16 + localX, chunkZ * 16 + localZ, stoneNoiseArray[localZ + localX * 16]);
            }
        }
        rockLayer1 = GenLayerTFC.initializeRock(world.getSeed() + 1, RockCategory.Layer.TOP, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        rockLayer2 = GenLayerTFC.initializeRock(world.getSeed() + 2, RockCategory.Layer.MIDDLE, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        rockLayer3 = GenLayerTFC.initializeRock(world.getSeed() + 3, RockCategory.Layer.BOTTOM, 5).getInts(chunkX * 16, chunkZ * 16, 16, 16).clone();
        for (int localX = 0; localX < 16; ++localX)
        {
            for (int localZ = 0; localZ < 16; ++localZ)
            {
                int colIndex = localX << 4 | localZ;

                Rock rock1 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer1[colIndex]);
                Rock rock2 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer2[colIndex]);
                Rock rock3 = ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(rockLayer3[colIndex]);

                primer.replaceBlockStates(biomes[localZ + localX * 16], localX, localZ, rock1, rock2, rock3);
            }
        }
    }

    private void prepareChunk(int chunkX, int chunkZ, ChunkPrimer primer)
    {
        // get noise values for the whole chunk
        populateNoiseArray(chunkX, chunkZ);

        double oneEighth = 0.125D;
        double oneQuarter = 0.25D;

        // entire chunk is 16x256x16
        // process chunk in subchunks, each one 4x8x4 blocks in size
        // 4 subchunks in x direction, each 4 blocks long
        // 32 subchunks in y direction, each 8 blocks long
        // 4 subchunks in z direction, each 4 blocks long
        // for a total of 512 subchunks

        // divide chunk into 4 subchunks in x direction, index as ix
        for (int ix = 0; ix < 4; ++ix)
        {
            int k_x0 = ix * 5;
            int k_x1 = (ix + 1) * 5;
            // divide chunk into 4 subchunks in z direction, index as iz
            for (int iz = 0; iz < 4; ++iz)
            {
                int k_x0z0 = (k_x0 + iz) * 33;
                int k_x0z1 = (k_x0 + iz + 1) * 33;
                int k_x1z0 = (k_x1 + iz) * 33;
                int k_x1z1 = (k_x1 + iz + 1) * 33;
                // divide chunk into 32 subchunks in y direction, index as iy
                for (int iy = 0; iy < 32; ++iy)
                {
                    // get the noise values from the noise array
                    // these are the values at the corners of the subchunk
                    double n_x0y0z0 = noiseArray[k_x0z0 + iy];
                    double n_x0y0z1 = noiseArray[k_x0z1 + iy];
                    double n_x1y0z0 = noiseArray[k_x1z0 + iy];
                    double n_x1y0z1 = noiseArray[k_x1z1 + iy];
                    double n_x0y1z0 = noiseArray[k_x0z0 + iy + 1];
                    double n_x0y1z1 = noiseArray[k_x0z1 + iy + 1];
                    double n_x1y1z0 = noiseArray[k_x1z0 + iy + 1];
                    double n_x1y1z1 = noiseArray[k_x1z1 + iy + 1];

                    // linearly interpolate between the noise points to get a noise value for each block in the subchunk

                    double noiseStepY00 = (n_x0y1z0 - n_x0y0z0) * oneEighth;
                    double noiseStepY01 = (n_x0y1z1 - n_x0y0z1) * oneEighth;
                    double noiseStepY10 = (n_x1y1z0 - n_x1y0z0) * oneEighth;
                    double noiseStepY11 = (n_x1y1z1 - n_x1y0z1) * oneEighth;

                    double noiseStartX0 = n_x0y0z0;
                    double noiseStartX1 = n_x0y0z1;
                    double noiseEndX0 = n_x1y0z0;
                    double noiseEndX1 = n_x1y0z1;
                    // subchunk is 8 blocks high in y direction, index as jy
                    for (int jy = 0; jy < 8; ++jy)
                    {
                        double noiseStartZ = noiseStartX0;
                        double noiseEndZ = noiseStartX1;

                        double noiseStepX0 = (noiseEndX0 - noiseStartX0) * oneQuarter;
                        double noiseStepX1 = (noiseEndX1 - noiseStartX1) * oneQuarter;
                        // subchunk is 4 blocks long in x direction, index as jx
                        for (int jx = 0; jx < 4; ++jx)
                        {
                            double noiseStepZ = (noiseEndZ - noiseStartZ) * oneQuarter;
                            double noiseVal = noiseStartZ;

                            // subchunk is 4 blocks long in x direction, index as jz
                            for (int jz = 0; jz < 4; ++jz)
                            {
                                // If the noise value is above zero, this block starts as stone
                                // Otherwise it's 'empty' - air above sealevel and water below it
                                if (noiseVal > 0.0D)
                                {
                                    primer.setBlockState(ix * 4 + jx, iy * 8 + jy, iz * 4 + jz, STONE);
                                }
                                else if (iy * 8 + jy < worldSettings.seaLevel)
                                {
                                    primer.setBlockState(ix * 4 + jx, iy * 8 + jy, iz * 4 + jz, WATER);
                                }
                                noiseVal += noiseStepZ;
                            }
                            noiseStartZ += noiseStepX0;
                            noiseEndZ += noiseStepX1;
                        }
                        noiseStartX0 += noiseStepY00;
                        noiseStartX1 += noiseStepY01;
                        noiseEndX0 += noiseStepY10;
                        noiseEndX1 += noiseStepY11;
                    }
                }
            }
        }
    }


    private TerrainSettings getWeightedTerrainSettings(int localX, int localZ, Biome[] biomes)
    {
        // Rivers shouldn't be influenced by the neighbors
        Biome centerBiome = biomes[localX + 2 + (localZ + 2) * 10];
        if (centerBiome == Biomes.RIVER || centerBiome == Biomes.FROZEN_RIVER || ((centerBiome instanceof BOPOverworldBiome) && ((BOPOverworldBiome) centerBiome).noNeighborTerrainInfuence))
        {
            return biomeTerrainSettings.get(centerBiome);
        }
        // Otherwise, get weighted average of properties from this and surrounding biomes
        TerrainSettings settings = new TerrainSettings();
        for (int i = -2; i <= 2; ++i)
        {
            for (int j = -2; j <= 2; ++j)
            {
                float weight = radialFalloff5x5[i + 2 + (j + 2) * 5];
                TerrainSettings biomeSettings = biomeTerrainSettings.get(biomes[localX + i + 2 + (localZ + j + 2) * 10]);

                if (biomeSettings != null)
                {
                    settings.avgHeight += weight * biomeSettings.avgHeight;
                    settings.variationAbove += weight * biomeSettings.variationAbove;
                    settings.variationBelow += weight * biomeSettings.variationBelow;
                    settings.minHeight += weight * biomeSettings.minHeight;
                    settings.maxHeight += weight * biomeSettings.maxHeight;
                    settings.sidewaysNoiseAmount += weight * biomeSettings.sidewaysNoiseAmount;
                    for (int k = 0; k < settings.octaveWeights.length; k++)
                    {
                        settings.octaveWeights[k] += weight * biomeSettings.octaveWeights[k];
                    }
                }
            }
        }
        return settings;
    }

    private Biome[] populateNoiseArray(int chunkX, int chunkZ)
    {
        Biome[] biomes = world.getBiomeProvider().getBiomesForGeneration(null, chunkX * 4 - 2, chunkZ * 4 - 2, 10, 10);

        // values from vanilla
        float coordinateScale = worldSettings.coordinateScale;
        float heightScale = worldSettings.heightScale;
        double upperLimitScale = worldSettings.upperLimitScale;
        double lowerLimitScale = worldSettings.lowerLimitScale;
        float mainNoiseScaleX = worldSettings.mainNoiseScaleX;
        float mainNoiseScaleY = worldSettings.mainNoiseScaleY;
        float mainNoiseScaleZ = worldSettings.mainNoiseScaleZ;

        int subchunkX = chunkX * 4;
        int subchunkY = 0;
        int subchunkZ = chunkZ * 4;

        // generate the xz noise for the chunk
        byteNoiseGen.generateNoise(subchunkX, subchunkZ);

        // generate the xyz noise for the chunk
        xyzBalanceNoiseArray = xyzBalanceNoiseGen.generateNoiseOctaves(xyzBalanceNoiseArray, subchunkX, subchunkY, subchunkZ, 5, 33, 5, coordinateScale / mainNoiseScaleX, heightScale / mainNoiseScaleY, (coordinateScale / mainNoiseScaleZ));
        xyzNoiseArrayA = xyzNoiseGenA.generateNoiseOctaves(xyzNoiseArrayA, subchunkX, subchunkY, subchunkZ, 5, 33, 5, coordinateScale, heightScale, coordinateScale);
        xyzNoiseArrayB = xyzNoiseGenB.generateNoiseOctaves(xyzNoiseArrayB, subchunkX, subchunkY, subchunkZ, 5, 33, 5, coordinateScale, heightScale, coordinateScale);

        // loop over the subchunks and calculate the overall noise value
        int xyzCounter = 0;
        int xzCounter = 0;
        for (int ix = 0; ix < 5; ++ix)
        {
            for (int iz = 0; iz < 5; ++iz)
            {
                // get the terrain settings to use for this subchunk as a weighted average of the settings from the nearby biomes
                TerrainSettings settings = getWeightedTerrainSettings(ix, iz, biomes);

                // get the xz noise value
                double xzNoiseVal = byteNoiseGen.getWeightedDouble(xzCounter, settings.octaveWeights);

                // get the amplitudes
                double xzAmplitude = worldSettings.amplitude * (xzNoiseVal < 0 ? settings.variationBelow : settings.variationAbove) * (1 - settings.sidewaysNoiseAmount);
                double xyzAmplitude = worldSettings.amplitude * (xzNoiseVal < 0 ? settings.variationBelow : settings.variationAbove) * (settings.sidewaysNoiseAmount);

                // the 'base level' is the average height, plus the height from the xz noise
                double baseLevel = settings.avgHeight + (xzNoiseVal * xzAmplitude);

                for (int iy = 0; iy < 33; ++iy)
                {
                    int y = iy * 8;

                    if (y < settings.minHeight)
                    {
                        noiseArray[xyzCounter] = settings.minHeight - y;
                    }
                    else if (y > settings.maxHeight)
                    {
                        noiseArray[xyzCounter] = settings.maxHeight - y;
                    }
                    else
                    {
                        // calculate the xzy noise value
                        double xyzNoiseA = xyzNoiseArrayA[xyzCounter] / lowerLimitScale;
                        double xyzNoiseB = xyzNoiseArrayB[xyzCounter] / upperLimitScale;
                        double balance = (xyzBalanceNoiseArray[xyzCounter] / 10.0D + 1.0D) / 2.0D;
                        double xyzNoiseValue = MathHelper.clamp(xyzNoiseA, xyzNoiseB, balance) / 50.0D;

                        // calculate the depth
                        double depth = baseLevel - y + (xyzAmplitude * xyzNoiseValue);

                        // make the noiseVal decrease sharply when we're close to the top of the chunk
                        // guarantees value of -10 at iy=32, so that there is always some air at the top
                        if (iy > 29)
                        {
                            double closeToTopOfChunkFactor = (float)(iy - 29) / 3.0F; // 1/3, 2/3 or 1
                            depth = depth * (1.0D - closeToTopOfChunkFactor) + -10.0D * closeToTopOfChunkFactor;
                        }
                        noiseArray[xyzCounter] = depth;
                    }
                    ++xyzCounter;
                }
                xzCounter++;
            }
        }
        return biomes;
    }

    private class FakeMansionChunkProvider extends ChunkGeneratorOverworld
    {
        public FakeMansionChunkProvider()
        {
            super(world, 0, true, "");
        }

        @Override
        public void setBlocksInChunk(int x, int z, ChunkPrimer primer)
        {
            prepareChunk(x, z, primer);
        }
    }
}
