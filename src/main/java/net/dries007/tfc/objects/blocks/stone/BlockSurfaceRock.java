package net.dries007.tfc.objects.blocks.stone;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.block.model.WeightedBakedModel;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.dries007.tfc.TerraFirmaCraft;
import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import net.dries007.tfc.client.model.ModelSurfaceRock;
import net.dries007.tfc.objects.blocks.property.RockProperty;
import net.dries007.tfc.objects.items.rock.ItemRock;
import zone.rong.zairyou.api.block.itemblock.MetaItemBlock;
import zone.rong.zairyou.api.block.metablock.AbstractMetaBlock;
import zone.rong.zairyou.api.block.metablock.MutableMetaBlockBuilder;
import zone.rong.zairyou.api.client.Bakery;
import zone.rong.zairyou.api.client.IModelOverride;
import zone.rong.zairyou.api.ore.block.SurfaceOreRockBlock;

public class BlockSurfaceRock extends AbstractMetaBlock<RockProperty, Rock, MetaItemBlock> implements IModelOverride
{
    public static final Map<Rock, BlockSurfaceRock> MAP;

    static
    {
        MAP = new MutableMetaBlockBuilder<>("surface_rock", BlockSurfaceRock.class, Material.ROCK, RockProperty.class)
            .entries(TFCRegistries.ROCKS.getValuesCollection()).build();
    }

    public static IBlockState get(Rock rock)
    {
        BlockSurfaceRock block = MAP.get(rock);
        return block.getDefaultState().withProperty(block.freezableProperty, rock);
    }

    public static Collection<BlockSurfaceRock> getInstances() {
        return new ObjectOpenHashSet<>(MAP.values().toArray(new BlockSurfaceRock[0]));
    }

    public BlockSurfaceRock(Material material, RockProperty property)
    {
        super(material, property);
        this.setHardness(0.1F);
        this.setSoundType(SoundType.STONE);
    }

    @Override
    public Supplier<MetaItemBlock> getItemBlock()
    {
        return () -> null;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        return ItemRock.get(state.getValue(freezableProperty));
    }

    @Override
    public int damageDropped(IBlockState state)
    {
        return 0;
    }

    @Override
    public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos)
    {
        return world.isSideSolid(pos.down(), EnumFacing.UP) && super.canPlaceBlockAt(world, pos);
    }

    @Override
    public boolean isReplaceable(IBlockAccess world, @Nonnull BlockPos pos)
    {
        return true;
    }

    @Override
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        if (!world.isSideSolid(pos.down(), EnumFacing.UP)) {
            world.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            world.destroyBlock(pos, true);
        }
        return true;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, @Nonnull BlockPos pos, @Nonnull EntityPlayer player) {
        return true;
    }

    @Override
    @Nonnull
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(ItemRock.get(state.getValue(freezableProperty)));
    }

    @Nonnull
    @Override
    public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    @Nonnull
    @Override
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return SurfaceOreRockBlock.GROUNDCOVER_AABB;
    }

    @Nullable
    @Override
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isSideSolid(IBlockState state, @Nonnull IBlockAccess world, @Nonnull BlockPos pos, EnumFacing side) {
        return false;
    }

    @Override
    public boolean isFullBlock(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }

    @Override
    public boolean addLandingEffects(IBlockState state, WorldServer world, BlockPos blockPosition, IBlockState iblockstate, EntityLivingBase entity, int numberOfParticles) {
        return true;
    }

    @Override
    public boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
        return true;
    }

    @Override
    public void addTextures(Set<ResourceLocation> set) { }

    @Override
    public void onModelRegister()
    {
        ModelLoader.setCustomStateMapper(this, new StateMapperBase()
        {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state)
            {
                return new ModelResourceLocation(state.getBlock().getRegistryName().toString() + "_" + state.getValue(freezableProperty).getRegistryName().toString());
            }
        });
    }

    @Override
    public void onModelBake(ModelBakeEvent event)
    {
        Bakery.ModelType[] modelTypes = new Bakery.ModelType[4];
        for (int i = 0; i < 4; i++)
        {
            modelTypes[i] = new Bakery.ModelType(TerraFirmaCraft.MOD_ID, "block/surface_rock/" + i);
        }
        final ModelRotation[] rots = ModelRotation.values();
        this.blockState.getValidStates().forEach(s ->
        {
            Rock rock = s.getValue(freezableProperty);
            WeightedBakedModel.Builder builder = new WeightedBakedModel.Builder();
            for (Bakery.ModelType modelType : modelTypes)
            {
                for (int i = 0; i < 4; i++)
                {
                    final int j = i;
                    builder.add(Bakery.INSTANCE.getBlockDepartment()
                        .template(modelType)
                        .prepareTexture("layer0", new ResourceLocation(TerraFirmaCraft.MOD_ID, "blocks/stonetypes/raw/" + rock.toString()))
                        .mutate(m -> new ModelSurfaceRock(m, rots[j]))
                        .bake()
                        .take(), 1);
                }
            }
            event.getModelRegistry().putObject(new ModelResourceLocation(this.getRegistryName().toString() + "_" + rock.getRegistryName().toString()), builder.build());
        });
    }
}
