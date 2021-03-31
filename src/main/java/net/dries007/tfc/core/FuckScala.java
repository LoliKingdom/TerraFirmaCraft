package net.dries007.tfc.core;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.registries.ForgeRegistry;

import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.objects.blocks.stone.BlockRockVariant;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataProvider;
import net.dries007.tfc.world.classic.chunkdata.ChunkDataTFC;
import net.dries007.tfc.world.classic.worldgen.experimental.NewExperimentalGenerator;
import org.objectweb.asm.*;

import scala.Tuple2;

public class FuckScala implements IClassTransformer
{
    public static Tuple2<Block, Integer> redirect$grassBlockFor(int x, int z, IBlockAccess world)
    {
        Rock rock = getRock(x, z, world);
        return rock == null ? new Tuple2<>(Blocks.AIR, 0) : new Tuple2<>(BlockRockVariant.get(rock, Rock.Type.GRASS), 0);
    }

    public static Tuple2<Block, Integer> redirect$dirtBlockFor(int x, int z, IBlockAccess world)
    {
        Rock rock = getRock(x, z, world);
        return rock == null ? new Tuple2<>(Blocks.AIR, 0) : new Tuple2<>(BlockRockVariant.get(rock, Rock.Type.DIRT), 0);
    }

    public static Tuple2<Block, Integer> redirect$sandBlockFor(int x, int z, IBlockAccess world)
    {
        Rock rock = getRock(x, z, world);
        return rock == null ? new Tuple2<>(Blocks.AIR, 0) : new Tuple2<>(BlockRockVariant.get(rock, Rock.Type.SAND), 0);
    }

    public static Tuple2<Block, Integer> redirect$gravelBlockFor(int x, int z, IBlockAccess world)
    {
        Rock rock = getRock(x, z, world);
        return rock == null ? new Tuple2<>(Blocks.AIR, 0) : new Tuple2<>(BlockRockVariant.get(rock, Rock.Type.GRAVEL), 0);
    }

    public static Tuple2<Block, Integer> redirect$rockBlockFor(int x, int z, IBlockAccess world)
    {
        Rock rock = getRock(x, z, world);
        return rock == null ? new Tuple2<>(Blocks.AIR, 0) : new Tuple2<>(BlockRockVariant.get(rock, Rock.Type.RAW), 0);
    }

    @Nullable
    private static Rock getRock(int x, int z, IBlockAccess world)
    {
        int localX = x >> 4;
        int localZ = z >> 4;
        int[] layer = NewExperimentalGenerator.rockLayerCache.get(new ChunkPos(localX, localZ));
        if (layer == null)
        {
            if (world instanceof World)
            {
                ChunkDataTFC chunkData = ((World) world).getCapability(ChunkDataProvider.CHUNK_DATA_CAPABILITY, null);
                return chunkData == null ? null : chunkData.getRock1(x, z);
            }
            return null;
        }
        int id = layer[((x & 15) << 4) | (z & 15)];
        return ((ForgeRegistry<Rock>) TFCRegistries.ROCKS).getValue(id);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes)
    {
        if (transformedName.equals("farseek.world.biome.package$"))
        {
            return fuckBiomePackage(bytes);
        }
        return bytes;
    }

    private byte[] fuckBiomePackage(byte[] bytes)
    {
        ClassReader reader = new ClassReader(bytes);
        ClassWriter writer = new ClassWriter(reader, 0);

        reader.accept(new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
            {
                if (name.endsWith("For"))
                {
                    MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                    return new MethodVisitor(Opcodes.ASM5, methodVisitor) {
                        @Override
                        public void visitCode()
                        {
                            mv.visitCode();
                            mv.visitVarInsn(Opcodes.ILOAD, 1);
                            mv.visitVarInsn(Opcodes.ILOAD, 3);
                            mv.visitVarInsn(Opcodes.ALOAD, 4);
                            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "net/dries007/tfc/core/FuckScala", "redirect$" + name, "(IILnet/minecraft/world/IBlockAccess;)Lscala/Tuple2;", false);
                            mv.visitInsn(Opcodes.ARETURN);
                            mv.visitMaxs(2, 6);
                            mv.visitEnd();
                        }
                    };
                }
                return super.visitMethod(access, name, desc, signature, exceptions);
            }
        }, 0);
        return writer.toByteArray();
    }
}
