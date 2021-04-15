package net.dries007.tfc.world.classic.worldgen.experimental;

import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;

import net.minecraftforge.fml.common.registry.ForgeRegistries;

import biomesoplenty.api.biome.BOPBiomes;
import biomesoplenty.api.biome.IExtendedBiome;
import biomesoplenty.api.generation.Generators;
import biomesoplenty.common.world.BOPWorldSettings;
import biomesoplenty.common.world.BiomeProviderBOP;
import biomesoplenty.common.world.WorldTypeBOP;

public class NewBiomeProvider extends BiomeProvider
{
    public static final BOPWorldSettings settings; // Relocate to our own WorldType

    static
    {
        settings = new BOPWorldSettings();
        settings.seaLevel = 64; // 96
        settings.amplitude = 2.4F;
        settings.biomeSize = BOPWorldSettings.BiomeSize.SMALL;
        settings.generateBopGems = false;
        settings.generateNetherHives = false;
        settings.dungeonChance = 4;
    }

    public NewBiomeProvider(long seed, WorldType worldType)
    {
        super();
        for (Biome biome : ForgeRegistries.BIOMES)
        {
            IExtendedBiome extendedBiome = BOPBiomes.REG_INSTANCE.getExtendedBiome(biome);
            if (extendedBiome != null)
            {
                extendedBiome.applySettings(settings);
            }
        }
        GenLayer[] layers = BiomeProviderBOP.setupBOPGenLayers(seed, settings);
        layers = this.getModdedBiomeGenerators(worldType, seed, layers);
        this.genBiomes = Generators.biomeGenLayer = layers[0];
        this.biomeIndexLayer = Generators.biomeIndexLayer = layers[1];
    }


}
