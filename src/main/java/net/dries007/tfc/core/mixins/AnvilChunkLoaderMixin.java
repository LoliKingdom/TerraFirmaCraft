package net.dries007.tfc.core.mixins;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.Logger;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin
{
    @Shadow protected abstract void writeChunkToNBT(Chunk chunkIn, World worldIn, NBTTagCompound compound);
    @Shadow protected abstract void addChunkToPending(ChunkPos pos, NBTTagCompound compound);

    @Shadow @Final private static Logger LOGGER;

    /**
     * @author Rongmario
     * @reason Offload saveChunk to another thread
     */
    @Overwrite
    public void saveChunk(World worldIn, Chunk chunkIn) throws MinecraftException
    {
        worldIn.checkSessionLock();
        try
        {
            NBTTagCompound tagToPass = new NBTTagCompound();
            List<NextTickListEntry> list = worldIn.getPendingBlockUpdates(chunkIn, false);
            if (list != null)
            {
                long j = worldIn.getTotalWorldTime();
                NBTTagList tileTickList = new NBTTagList();
                for (NextTickListEntry nextticklistentry : list)
                {
                    NBTTagCompound tileTag = new NBTTagCompound();
                    ResourceLocation resourcelocation = nextticklistentry.getBlock().getRegistryName();
                    tileTag.setString("i", resourcelocation == null ? "" : resourcelocation.toString());
                    tileTag.setInteger("x", nextticklistentry.position.getX());
                    tileTag.setInteger("y", nextticklistentry.position.getY());
                    tileTag.setInteger("z", nextticklistentry.position.getZ());
                    tileTag.setInteger("t", (int)(nextticklistentry.scheduledTime - j));
                    tileTag.setInteger("p", nextticklistentry.priority);
                    tileTickList.appendTag(tileTag);
                }
                tagToPass.setTag("TileTicks", tileTickList);
            }
            ForkJoinPool.commonPool().execute(() ->
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                final NBTTagCompound nbttagcompound1 = tagToPass;
                final Chunk chunk = chunkIn;
                final World world = worldIn;

                nbttagcompound.setTag("Level", nbttagcompound1);
                nbttagcompound.setInteger("DataVersion", 1343);
                FMLCommonHandler.instance().getDataFixer().writeVersionData(nbttagcompound);
                this.writeChunkToNBT(chunk, world, nbttagcompound1); // This is the tick time sink

                world.getMinecraftServer().addScheduledTask(() ->
                {
                    ForgeChunkManager.storeChunkNBT(chunk, nbttagcompound1);
                    MinecraftForge.EVENT_BUS.post(new ChunkDataEvent.Save(chunk, nbttagcompound));
                    this.addChunkToPending(chunk.getPos(), nbttagcompound);
                });
            });
        }
        catch (Exception exception)
        {
            LOGGER.error("Failed to save chunk", exception);
        }
    }

    @Redirect(method = "writeChunkToNBT", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getPendingBlockUpdates(Lnet/minecraft/world/chunk/Chunk;Z)Ljava/util/List;"))
    private List<NextTickListEntry> neverEver(World world, Chunk chunkIn, boolean remove)
    {
        return null;
    }

}
