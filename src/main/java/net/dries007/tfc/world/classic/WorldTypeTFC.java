/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.world.classic;

import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCreateWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import biomesoplenty.common.world.BOPWorldSettings;
import mcp.MethodsReturnNonnullByDefault;
import net.dries007.tfc.world.classic.worldgen.experimental.NewBiomeProvider;
import net.dries007.tfc.world.classic.worldgen.experimental.NewExperimentalGenerator;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WorldTypeTFC extends WorldType
{
    @Deprecated public static final int SEALEVEL = 144;
    @Deprecated public static final int ROCKLAYER2 = 110;
    @Deprecated public static final int ROCKLAYER3 = 55;

    public static final BOPWorldSettings settings; // Relocate to our own WorldType

    static
    {
        settings = new BOPWorldSettings();
        settings.seaLevel = 95;
        settings.amplitude = 1.8F;
        settings.biomeSize = BOPWorldSettings.BiomeSize.MEDIUM;
        settings.landScheme = BOPWorldSettings.LandMassScheme.CONTINENTS;
        settings.rainScheme = BOPWorldSettings.RainfallVariationScheme.MEDIUM_ZONES;
        settings.tempScheme = BOPWorldSettings.TemperatureVariationScheme.LATITUDE;
        settings.mainNoiseScaleX = 80.0F;
        settings.mainNoiseScaleY = 160.0F;
        settings.mainNoiseScaleZ = 80.0F;
        settings.coordinateScale = 684.412F;
        settings.heightScale = 684.412F;
        settings.upperLimitScale = 512.0F;
        settings.generateBopGems = false;
        settings.generateNetherHives = false;
        settings.dungeonChance = 4;
    }

    public WorldTypeTFC()
    {
        super("tfc_classic");
    }

    @Override
    public BiomeProvider getBiomeProvider(World world)
    {
        return new NewBiomeProvider(world.getSeed(), this);
        // return new BiomeProviderTFC(world);
    }

    @Override
    public IChunkGenerator getChunkGenerator(World world, String generatorOptions)
    {
        return new NewExperimentalGenerator(world);
        // return new ChunkGenTFC(world, generatorOptions);
    }

    @Override
    public int getMinimumSpawnHeight(World world)
    {
        return settings.seaLevel;
    }

    @Override
    public double getHorizon(World world)
    {
        return settings.seaLevel;
    }

    @Override
    public int getSpawnFuzz(WorldServer world, MinecraftServer server)
    {
        if (world.getGameRules().hasRule("spawnRadius")) return world.getGameRules().getInt("spawnRadius");
        return ((ChunkGenTFC) world.getChunkProvider().chunkGenerator).s.spawnFuzz;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld)
    {
        // mc.displayGuiScreen(new GuiCustomizeWorld(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson));
    }

    @Override
    public boolean isCustomizable()
    {
        return false;
    }

    @Override
    public float getCloudHeight()
    {
        return settings.seaLevel * 2;
    }
}
