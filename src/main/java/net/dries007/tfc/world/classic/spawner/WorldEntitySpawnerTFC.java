/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic.spawner;

import java.lang.ref.SoftReference;
import java.util.*;
import java.util.function.IntSupplier;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.api.types.ICreatureTFC;
import net.dries007.tfc.objects.entity.animal.*;
import net.dries007.tfc.util.calendar.CalendarTFC;
import net.dries007.tfc.util.calendar.ICalendar;
import net.dries007.tfc.util.climate.ClimateTFC;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;

import static net.dries007.tfc.TerraFirmaCraft.MOD_ID;

/*
 * TFC entity spawning mechanics
 * Only works in tfc type worlds
 */
@SuppressWarnings("WeakerAccess")
@Mod.EventBusSubscriber(modid = MOD_ID)
public final class WorldEntitySpawnerTFC
{
    /**
     * Handles livestock cooldown time
     * Supplier so we get the updated config value
     */
    public static final Map<Class<? extends EntityLiving>, IntSupplier> LIVESTOCK;

    static SoftReference<List<EntityEntry>> tfcCreatures;

    static
    {
        LIVESTOCK = new HashMap<>();
        LIVESTOCK.put(EntityAlpacaTFC.class, () -> ConfigTFC.Animals.ALPACA.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityCamelTFC.class, () -> ConfigTFC.Animals.CAMEL.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityChickenTFC.class, () -> ConfigTFC.Animals.CHICKEN.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityCowTFC.class, () -> ConfigTFC.Animals.COW.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityDonkeyTFC.class, () -> ConfigTFC.Animals.DONKEY.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityDuckTFC.class, () -> ConfigTFC.Animals.DUCK.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityGoatTFC.class, () -> ConfigTFC.Animals.GOAT.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityGrouseTFC.class, () -> ConfigTFC.Animals.GROUSE.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityHorseTFC.class, () -> ConfigTFC.Animals.HORSE.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityLlamaTFC.class, () -> ConfigTFC.Animals.LLAMA.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityMuskOxTFC.class, () -> ConfigTFC.Animals.MUSKOX.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityOcelotTFC.class, () -> ConfigTFC.Animals.OCELOT.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityPigTFC.class, () -> ConfigTFC.Animals.PIG.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityQuailTFC.class, () -> ConfigTFC.Animals.QUAIL.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntitySheepTFC.class, () -> ConfigTFC.Animals.SHEEP.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityWolfTFC.class, () -> ConfigTFC.Animals.WOLF.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityYakTFC.class, () -> ConfigTFC.Animals.YAK.elder * ICalendar.TICKS_IN_DAY);
        LIVESTOCK.put(EntityZebuTFC.class, () -> ConfigTFC.Animals.ZEBU.elder * ICalendar.TICKS_IN_DAY);
    }

    public static void init()
    {
        EnumCreatureType.MONSTER.maxNumberOfCreature = ConfigTFC.General.DIFFICULTY.mobSpawnCount;
        EnumCreatureType.CREATURE.maxNumberOfCreature = ConfigTFC.General.DIFFICULTY.animalSpawnCount;
        // Using enum helper to add creature types adds more issues than resolve.
        // Although it worked in dev and with only minor mods, I had too much trouble with a larger modpack
    }

    /**
     * Experimental: Handles wild livestock respawning
     * This event runs after CheckSpawn, which means you can safely assume that all other restrictions passed (biome, temp, rainfall, etc)
     */
    @SubscribeEvent
    public static void onLivestockRespawn(LivingSpawnEvent.SpecialSpawn event)
    {
        EntityLiving entity = (EntityLiving) event.getEntity();
        Class<? extends EntityLiving> entityClass = entity.getClass();
        if (LIVESTOCK.containsKey(entityClass))
        {
            World worldIn = event.getWorld();
            event.setResult(Event.Result.ALLOW); // Always cancel vanilla's spawning since we take it from here
            AnimalRespawnWorldData data = AnimalRespawnWorldData.get(worldIn);
            ChunkPos pos = new ChunkPos(((int) event.getX()) >> 4, ((int) event.getZ()) >> 4);
            long lastSpawnTick = data.getLastRespawnTick(entity, pos);
            if (lastSpawnTick <= 0 || LIVESTOCK.get(entityClass).getAsInt() <= CalendarTFC.PLAYER_TIME.getTicks() - lastSpawnTick)
            {
                data.setLastRespawnTick(entity, pos, CalendarTFC.PLAYER_TIME.getTicks());
                //noinspection ConstantConditions
                doGroupSpawning(EntityRegistry.getEntry(entityClass), entity, worldIn, (int) event.getX(), (int) event.getZ(), 16, 16, worldIn.rand); // centerX, centerZ, diameterX, diameterZ
            }
        }
    }

    /**
     * **Modified version from vanilla's {@link net.minecraft.world.WorldEntitySpawner}
     * Called during chunk generation to spawn initial creatures.
     * Spawns group of animals together
     *
     * @param centerX   The X coordinate of the point to spawn mobs around.
     * @param centerZ   The Z coordinate of the point to spawn mobs around.
     * @param diameterX The X diameter of the rectangle to spawn mobs in
     * @param diameterZ The Z diameter of the rectangle to spawn mobs in
     */
    public static void performWorldGenSpawning(World worldIn, Biome biomeIn, int centerX, int centerZ, int diameterX, int diameterZ, Random randomIn)
    {
        final BlockPos chunkBlockPos = new BlockPos(centerX, 0, centerZ);

        final float temperature = ClimateTFC.getAvgTemp(worldIn, chunkBlockPos);
        final float rainfall = ChunkDataTFC.getRainfall(worldIn, chunkBlockPos);
        final float floraDensity = ChunkDataTFC.getFloraDensity(worldIn, chunkBlockPos);
        final float floraDiversity = ChunkDataTFC.getFloraDiversity(worldIn, chunkBlockPos);

        if (tfcCreatures == null || tfcCreatures.get() == null) {
            List<EntityEntry> refEntries = new ObjectArrayList<>();
            for (EntityEntry entry : ForgeRegistries.ENTITIES) {
                if (ICreatureTFC.class.isAssignableFrom(entry.getEntityClass())) {
                    refEntries.add(entry);
                }
            }
            tfcCreatures = new SoftReference<>(refEntries);
        }

        for (EntityEntry entry : tfcCreatures.get()) {
            ICreatureTFC entity = (ICreatureTFC) entry.newInstance(worldIn);
            int weight = entity.getSpawnWeight(biomeIn, temperature, rainfall, floraDensity, floraDiversity);
            if (weight > 0 && randomIn.nextInt(weight) == 0) {
                doGroupSpawning(entry, (EntityLiving) entity, worldIn, centerX, centerZ, diameterX, diameterZ, randomIn);
                break;
            }
        }
    }

    private static void doGroupSpawning(EntityEntry entry, EntityLiving livingEntity, World worldIn, int centerX, int centerZ, int diameterX, int diameterZ, Random randomIn) {
        final List<EntityLiving> group = new ObjectArrayList<>();
        ICreatureTFC creatureTFC = (ICreatureTFC) livingEntity;
        int fallback = 5; // Fallback measure if some mod completely deny this entity spawn
        int individuals = Math.max(1, creatureTFC.getMinGroupSize()) + randomIn.nextInt(creatureTFC.getMaxGroupSize() - Math.max(0, creatureTFC.getMinGroupSize() - 1));
        while (individuals > 0)
        {
            int j = centerX + randomIn.nextInt(diameterX);
            int k = centerZ + randomIn.nextInt(diameterZ);
            BlockPos blockpos = worldIn.getTopSolidOrLiquidBlock(new BlockPos(j, 0, k));
            livingEntity.setLocationAndAngles((float) j + 0.5F, blockpos.getY(), (float) k + 0.5F, randomIn.nextFloat() * 360.0F, 0.0F);
            if (livingEntity.getCanSpawnHere()) // fix entities spawning inside walls
            {
                if (ForgeEventFactory.canEntitySpawn(livingEntity, worldIn, j + 0.5f, (float) blockpos.getY(), k + 0.5f, null) == Event.Result.DENY)
                {
                    if (--fallback > 0)
                    {
                        continue;
                    }
                    else
                    {
                        break; // Someone doesn't want me to spawn :(
                    }
                }
                fallback = 5;
                // Spawn pass! let's continue
                worldIn.spawnEntity(livingEntity);
                group.add(livingEntity);
                livingEntity.onInitialSpawn(worldIn.getDifficultyForLocation(new BlockPos(livingEntity)), null);
                if (--individuals > 0)
                {
                    //We still need to spawn more
                    livingEntity = (EntityLiving) entry.newInstance(worldIn);
                    creatureTFC = (ICreatureTFC) livingEntity;
                }
            }
            else
            {
                if (--fallback <= 0) //Trying to spawn in water or inside walls too many times, let's break
                {
                    break;
                }
            }
        }
        // Apply the group spawning mechanics!
        creatureTFC.getGroupingRules().accept(group, randomIn);
    }

}
