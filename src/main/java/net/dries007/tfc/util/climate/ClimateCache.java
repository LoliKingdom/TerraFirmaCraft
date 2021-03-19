/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.util.climate;

import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/**
 * This stores climate data for when the world context is not available
 */
public final class ClimateCache
{
    private final Map<ChunkPos, ClimateData> backingMap = new Object2ObjectOpenHashMap<>();

    @Nonnull
    public ClimateData get(BlockPos pos)
    {
        return get(new ChunkPos(pos));
    }

    @Nonnull
    public ClimateData get(ChunkPos pos)
    {
        return backingMap.getOrDefault(pos, ClimateData.DEFAULT);
    }

    public void update(ChunkPos pos, float temperature, float rainfall)
    {
        backingMap.put(pos, new ClimateData(temperature, rainfall));
    }
}
