package net.dries007.tfc.core.mixins;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.logging.log4j.Logger;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin
{
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
                writeChunkToNBT$New(chunk, world, nbttagcompound1); // This is the tick time sink

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

    private void writeChunkToNBT$New(Chunk chunkIn, World worldIn, NBTTagCompound compound)
    {
        compound.setInteger("xPos", chunkIn.x);
        compound.setInteger("zPos", chunkIn.z);
        compound.setLong("LastUpdate", worldIn.getTotalWorldTime());
        compound.setIntArray("HeightMap", chunkIn.getHeightMap());
        compound.setBoolean("TerrainPopulated", chunkIn.isTerrainPopulated());
        compound.setBoolean("LightPopulated", chunkIn.isLightPopulated());
        compound.setLong("InhabitedTime", chunkIn.getInhabitedTime());
        ExtendedBlockStorage[] ebs = chunkIn.getBlockStorageArray();
        NBTTagList nbttaglist = new NBTTagList();
        boolean flag = worldIn.provider.hasSkyLight();

        for (ExtendedBlockStorage extendedblockstorage : ebs)
        {
            if (extendedblockstorage != Chunk.NULL_BLOCK_STORAGE)
            {
                NBTTagCompound nbttagcompound = new NBTTagCompound();
                nbttagcompound.setByte("Y", (byte)(extendedblockstorage.getYLocation() >> 4 & 255));
                byte[] abyte = new byte[4096];
                NibbleArray nibblearray = new NibbleArray();
                NibbleArray nibblearray1 = extendedblockstorage.getData().getDataForNBT(abyte, nibblearray);
                nbttagcompound.setByteArray("Blocks", abyte);
                nbttagcompound.setByteArray("Data", nibblearray.getData());
                if (nibblearray1 != null)
                {
                    nbttagcompound.setByteArray("Add", nibblearray1.getData());
                }
                nbttagcompound.setByteArray("BlockLight", extendedblockstorage.getBlockLight().getData());
                if (flag)
                {
                    nbttagcompound.setByteArray("SkyLight", extendedblockstorage.getSkyLight().getData());
                }
                else
                {
                    nbttagcompound.setByteArray("SkyLight", new byte[extendedblockstorage.getBlockLight().getData().length]);
                }
                nbttaglist.appendTag(nbttagcompound);
            }
        }
        compound.setTag("Sections", nbttaglist);
        compound.setByteArray("Biomes", chunkIn.getBiomeArray());
        chunkIn.setHasEntities(false);
        NBTTagList nbttaglist1 = new NBTTagList();
        for (int i = 0; i < chunkIn.getEntityLists().length; ++i)
        {
            for (Entity entity : chunkIn.getEntityLists()[i])
            {
                NBTTagCompound nbttagcompound2 = new NBTTagCompound();
                try
                {
                    if (entity.writeToNBTOptional(nbttagcompound2))
                    {
                        chunkIn.setHasEntities(true);
                        nbttaglist1.appendTag(nbttagcompound2);
                    }
                }
                catch (Exception e)
                {
                    FMLLog.log.error("An Entity type {} has thrown an exception trying to write state. It will not persist. Report this to the mod author", entity.getClass().getName(), e);
                }
            }
        }
        compound.setTag("Entities", nbttaglist1);
        NBTTagList nbttaglist2 = new NBTTagList();
        for (TileEntity tileentity : chunkIn.getTileEntityMap().values())
        {
            try
            {
                NBTTagCompound nbttagcompound3 = tileentity.writeToNBT(new NBTTagCompound());
                nbttaglist2.appendTag(nbttagcompound3);
            }
            catch (Exception e)
            {
                FMLLog.log.error("A TileEntity type {} has throw an exception trying to write state. It will not persist. Report this to the mod author", tileentity.getClass().getName(), e);
            }
        }
        compound.setTag("TileEntities", nbttaglist2);
        if (chunkIn.getCapabilities() != null)
        {
            try
            {
                compound.setTag("ForgeCaps", chunkIn.getCapabilities().serializeNBT());
            }
            catch (Exception exception)
            {
                FMLLog.log.error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
            }
        }
    }

}
