package net.dries007.tfc.world.classic.worldgen.experimental;

import java.util.Random;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.terraingen.ChunkGeneratorEvent;
import net.minecraftforge.event.terraingen.InitNoiseGensEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistry;

import biomesoplenty.common.world.WorldTypeBOP;
import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.api.types.RockCategory;
import net.dries007.tfc.util.calendar.Month;
import net.dries007.tfc.util.climate.ClimateHelper;
import net.dries007.tfc.world.classic.chunkdata.CapabilityChunkData;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;
import net.dries007.tfc.world.classic.genlayers.GenLayerTFC;

public class WorldGeneratorHook
{
    private static WorldGeneratorHook INSTANCE;

    public static WorldGeneratorHook getInstance()
    {
        if (INSTANCE == null)
        {
            INSTANCE = new WorldGeneratorHook();
        }
        return INSTANCE;
    }

    private Random seededRandom = null;
    private InitNoiseGensEvent.ContextOverworld contextOverworld = null;

    private GenLayerTFC firstRockGenLayer, secondRockGenLayer, thirdRockGenLayer;

    private NoiseGeneratorPerlin rainFallNoiseGen, floraDiversityNoiseGen, floraDensityNoiseGen, temperatureNoiseGen;

    private float rainfallSpread, floraDiversitySpread, floraDensitySpread;

    private final int[] firstRockLayer = new int[256];
    private final int[] secondRockLayer = new int[256];
    private final int[] thirdRockLayer = new int[256];

    private float rainfall, floraDiversity, floraDensity, regionalFactor, averageTemperature;

    private WorldGeneratorHook()
    {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.TERRAIN_GEN_BUS.register(this);
    }

    @SubscribeEvent
    @SuppressWarnings("ConstantConditions")
    public void onInitializingNoiseGen(InitNoiseGensEvent<InitNoiseGensEvent.ContextOverworld> event)
    {
        // Redundant my ass
        if (event.getOriginal() instanceof InitNoiseGensEvent.ContextOverworld)
        {
            seededRandom = event.getRandom();
            contextOverworld = event.getNewValues().clone();

            long seed = event.getWorld().getSeed();

            firstRockGenLayer = GenLayerTFC.initializeRock(seed + 1, RockCategory.Layer.TOP, 5); // Default 5 size
            secondRockGenLayer = GenLayerTFC.initializeRock(seed + 2, RockCategory.Layer.MIDDLE, 5);
            thirdRockGenLayer = GenLayerTFC.initializeRock(seed + 3, RockCategory.Layer.BOTTOM, 5);

            rainFallNoiseGen = new NoiseGeneratorPerlin(new Random(seed + 4), 4);
            floraDensityNoiseGen = new NoiseGeneratorPerlin(new Random(seed + 5), 4);
            floraDiversityNoiseGen = new NoiseGeneratorPerlin(new Random(seed + 6), 4);
            temperatureNoiseGen = new NoiseGeneratorPerlin(new Random(seed + 7), 4);

            rainfallSpread = (float) ConfigTFC.General.WORLD.rainfallSpreadFactor;
            floraDiversitySpread = (float) ConfigTFC.General.WORLD.floraDiversitySpreadFactor;
            floraDensitySpread = (float) ConfigTFC.General.WORLD.floraDensitySpreadFactor;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onBiomeTerrainGen(ChunkGeneratorEvent.ReplaceBiomeBlocks event)
    {
        if (event.getWorld() == null || seededRandom == null)
        {
            return;
        }

        // event.setResult(Event.Result.DENY);

        int chunkX = event.getX();
        int chunkZ = event.getZ();

        System.arraycopy(this.firstRockLayer, 0, firstRockGenLayer.getInts(chunkX * 16, chunkZ * 16, 16, 16), 0, 256);
        System.arraycopy(this.secondRockLayer, 0, secondRockGenLayer.getInts(chunkX * 16, chunkZ * 16, 16, 16), 0, 256);
        System.arraycopy(this.thirdRockLayer, 0, thirdRockGenLayer.getInts(chunkX * 16, chunkZ * 16, 16, 16), 0, 256);

        rainfall = MathHelper.clamp(250f + 250f * rainfallSpread * (float) rainFallNoiseGen.getValue(chunkX * 0.005, chunkZ * 0.005), 0, 500);
        floraDiversity = MathHelper.clamp(0.5f + 0.5f * floraDiversitySpread * (float) floraDiversityNoiseGen.getValue(chunkX * 0.005, chunkZ * 0.005), 0, 1);
        floraDensity = MathHelper.clamp((0.3f + 0.2f * rainfall / 500f) + 0.4f * floraDensitySpread * (float) floraDensityNoiseGen.getValue(chunkX * 0.05, chunkZ * 0.05), 0, 1);

        regionalFactor = 5f * 0.09f * (float) temperatureNoiseGen.getValue(chunkX * 0.05, chunkZ * 0.05); // Range -5 <> 5
        averageTemperature = ClimateHelper.monthFactor(regionalFactor, Month.AVERAGE_TEMPERATURE_MODIFIER, chunkZ << 4);

        /*

        double[] depthBuffer = new double[256];
        depthBuffer = this.contextOverworld.getHeight().getRegion(depthBuffer, chunkX * 16, chunkZ * 16, 16, 16, 0.0625D, 0.0625D, 1.0D);
        Biome[] biomes = event.getWorld().getBiomeProvider().getBiomes(null, chunkX * 16, chunkZ * 16, 16, 16);
        for (int i = 0; i < 16; ++i)
        {
            for (int j = 0; j < 16; ++j)
            {
                Biome biome = biomes[j + i * 16];
                generateBiomeTerrain(biome, event.getWorld(), this.seededRandom, event.getPrimer(), chunkX * 16 + i, chunkZ * 16 + j, depthBuffer[j + i * 16]);
            }
        }
         */
    }

    /*
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onCapabilityAttach$Chunk(AttachCapabilitiesEvent<Chunk> event)
    {
        if (seededRandom != null && event.getObject().getWorld().getWorldType() != TerraFirmaCraft.getWorldType())
        {
            // Get the Chunk here and we can capture capabilities early
            ChunkDataTFC data = event.getCapabilities().get(CapabilityChunkData.CHUNK_DATA).getCapability(ChunkDataProvider.CHUNK_DATA_CAPABILITY, null);
            data.setGenerationData(this.firstRockLayer, this.secondRockLayer, this.thirdRockLayer, this.rainfall, this.regionalFactor, this.averageTemperature, this.floraDensity, this.floraDiversity);
        }
        if (event.getObject().getWorld().getWorldType() instanceof WorldTypeBOP)
        {
            BlockPos centralPos = event.getObject().getPos().getBlock(8, 64, 8);
            Biome biome = event.getObject().getBiome(centralPos, event.getObject().getWorld().getBiomeProvider());
            TerraFirmaCraft.getLog().info("Biome: {} | CanRain: {} | Rainfall: {} | DefaultTemp: {} | CentralTemp: {} | HighHumidity: {} | EnableSnow {}",
                biome.getRegistryName(),
                biome.canRain(),
                biome.getRainfall(),
                biome.getDefaultTemperature(),
                biome.getTemperature(centralPos),
                biome.isHighHumidity(),
                biome.getEnableSnow());
        }
    }

     */

    /*
    private void generateBiomeTerrain(Biome biomeIn, World worldIn, Random rand, ChunkPrimer chunkPrimerIn, int x, int z, double noiseVal)
    {
        int i = worldIn.getSeaLevel();
        IBlockState topState = biomeIn.topBlock;
        IBlockState fillerState = biomeIn.fillerBlock;
        int j = -1;
        int k = (int) (noiseVal / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int l = x & 15;
        int i1 = z & 15;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int y = 255; y >= 0; --y)
        {
            if (y <= rand.nextInt(5))
            {
                chunkPrimerIn.setBlockState(i1, y, l, Blocks.BEDROCK.getDefaultState());
            }
            else
            {
                IBlockState iblockstate2 = chunkPrimerIn.getBlockState(i1, y, l);
                if (iblockstate2.getMaterial() == Material.AIR)
                {
                    j = -1;
                }
                else if (iblockstate2.getBlock() == Blocks.STONE)
                {
                    if (j == -1)
                    {
                        if (k <= 0)
                        {
                            topState = Blocks.AIR.getDefaultState();
                            fillerState = BlockRockVariant.get(getRockLayer1((x << 4) & 15, (z << 4) & 15), Rock.Type.RAW).getDefaultState();
                        }
                        else if (y >= i - 4 && y <= i + 1)
                        {
                            topState = BlockRockVariant.get(getRockLayer1((x << 4) & 15, (z << 4) & 15), Rock.Type.GRASS).getDefaultState();
                            fillerState = biomeIn.fillerBlock;
                        }

                        if (y < i && (topState == null || topState.getMaterial() == Material.AIR))
                        {
                            if (biomeIn.getTemperature(blockpos$mutableblockpos.setPos(x, y, z)) < 0.15F)
                            {
                                topState = Blocks.ICE.getDefaultState();
                            }
                            else
                            {
                                topState = Blocks.WATER.getDefaultState();
                            }
                        }
                        j = k;
                        if (y >= i - 1)
                        {
                            chunkPrimerIn.setBlockState(i1, y, l, topState);
                        }
                        else if (y < i - 7 - k)
                        {
                            topState = Blocks.AIR.getDefaultState();
                            fillerState = BlockRockVariant.get(getRockLayer1((x << 4) & 15, (z << 4) & 15), Rock.Type.RAW).getDefaultState();
                            chunkPrimerIn.setBlockState(i1, y, l, BlockRockVariant.get(getRockLayer1((x << 4) & 15, (z << 4) & 15), Rock.Type.GRAVEL).getDefaultState());
                        }
                        else
                        {
                            chunkPrimerIn.setBlockState(i1, y, l, fillerState);
                        }
                    }
                    else if (j > 0)
                    {
                        --j;
                        chunkPrimerIn.setBlockState(i1, y, l, fillerState);
                        if (j == 0 && fillerState.getBlock() == Blocks.SAND && k > 1)
                        {
                            j = rand.nextInt(4) + Math.max(0, y - worldIn.getSeaLevel() - 1); // previously 63
                            fillerState = fillerState.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? Blocks.RED_SANDSTONE.getDefaultState() : Blocks.SANDSTONE.getDefaultState();
                        }
                    }
                }
            }
        }
    }
     */

    private Rock getRockLayer1(int x, int z)
    {
        x = (x << 4) & 15;
        z = (z << 4) & 15;
        return ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(this.firstRockLayer[z << 4 | x]);
    }

    private Rock getRockLayer2(int x, int z)
    {
        x = (x << 4) & 15;
        z = (z << 4) & 15;
        return ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(this.secondRockLayer[z << 4 | x]);
    }

    private Rock getRockLayer3(int x, int z)
    {
        x = (x << 4) & 15;
        z = (z << 4) & 15;
        return ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(this.thirdRockLayer[z << 4 | x]);
    }

}
