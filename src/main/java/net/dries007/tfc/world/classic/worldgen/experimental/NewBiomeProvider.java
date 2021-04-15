package net.dries007.tfc.world.classic.worldgen.experimental;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;

import net.minecraftforge.fml.common.registry.ForgeRegistries;

import biomesoplenty.api.biome.BOPBiomes;
import biomesoplenty.api.biome.IExtendedBiome;
import biomesoplenty.api.generation.Generators;
import biomesoplenty.common.world.BiomeProviderBOP;
import net.dries007.tfc.world.classic.WorldTypeTFC;

public class NewBiomeProvider extends BiomeProvider
{
    public NewBiomeProvider(long seed, WorldType worldType)
    {
        super();
        for (Biome biome : ForgeRegistries.BIOMES)
        {
            IExtendedBiome extendedBiome = BOPBiomes.REG_INSTANCE.getExtendedBiome(biome);
            if (extendedBiome != null)
            {
                extendedBiome.applySettings(WorldTypeTFC.settings);
            }
        }
        GenLayer[] layers = BiomeProviderBOP.setupBOPGenLayers(seed, WorldTypeTFC.settings);
        layers = this.getModdedBiomeGenerators(worldType, seed, layers);
        this.genBiomes = Generators.biomeGenLayer = layers[0];
        this.biomeIndexLayer = Generators.biomeIndexLayer = layers[1];
    }
}
