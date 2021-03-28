/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic.genlayers.biome;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import javax.annotation.Nonnull;

import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.registries.ForgeRegistry;

import net.dries007.tfc.ConfigTFC;
import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.api.types.RockCategory;
import net.dries007.tfc.world.classic.genlayers.GenLayerTFC;

public class GenLayerBiomeSensitiveRockInit extends GenLayerTFC
{
    private final int[] layerRocks;

    public GenLayerBiomeSensitiveRockInit(long seed, Set<Biome> biomesInChunk, final BiomeSensitiveLayer rocks)
    {
        super(seed);
        layerRocks = TFCRegistries.ROCKS.getValuesCollection().stream()
            .filter(Rock::isNaturallyGenerating)
            .filter(rock -> rocks.test(biomesInChunk, rock))
            .mapToInt(((ForgeRegistry<Rock>) TFCRegistries.ROCKS)::getID)
            .sorted()
            .toArray();
        if (ConfigTFC.General.DEBUG.debugWorldGenSafe)
        {
            TerraFirmaCraft.getLog().info("Worldgen biomes: {}", (Object) biomesInChunk.toArray(new Biome[0]));
            TerraFirmaCraft.getLog().info("Worldgen rock list (ints): {}", layerRocks);
            TerraFirmaCraft.getLog().info("Worldgen rock list (names): {}", (Object) Arrays.stream(layerRocks).mapToObj(((ForgeRegistry<Rock>) TFCRegistries.ROCKS)::getValue).map(Objects::toString).toArray());
        }
    }

    @Override
    @Nonnull
    public int[] getInts(int par1, int par2, int maxX, int maxZ)
    {
        int[] cache = IntCache.getIntCache(maxX * maxZ);

        for (int z = 0; z < maxZ; ++z)
        {
            for (int x = 0; x < maxX; ++x)
            {
                this.initChunkSeed(par1 + x, par2 + z);
                cache[x + z * maxX] = layerRocks[this.nextInt(layerRocks.length)];
            }
        }

        return cache;
    }

    public enum BiomeSensitiveLayer implements BiPredicate<Set<Biome>, Rock>
    {
        BOTTOM(3),
        MIDDLE(2),
        TOP(1);

        public final int layer;

        BiomeSensitiveLayer(int layer)
        {
            this.layer = layer;
        }

        @Override
        public boolean test(Set<Biome> biomes, Rock rock)
        {
            RockCategory category = rock.getRockCategory();
            switch (this)
            {
                case TOP:
                    return category.canGenerateAsLayer(1) && category.canGenerateInBiomes(biomes);
                case MIDDLE:
                    return category.canGenerateAsLayer(2) && category.canGenerateInBiomes(biomes);
                case BOTTOM:
                    return category.canGenerateAsLayer(3) && category.canGenerateInBiomes(biomes);
                default:
                    throw new IllegalStateException("There's only 3 layers!");
            }
        }
    }
}
