package net.dries007.tfc.client.model;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.model.animation.IClip;

public class ModelSurfaceRock implements IModel
{
    private final IModel parentModel;
    private final ModelRotation rotation;

    public ModelSurfaceRock(IModel parentModel, ModelRotation rotation)
    {
        this.parentModel = parentModel;
        this.rotation = rotation;
    }

    @Override
    public IBakedModel bake(IModelState state, VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter)
    {
        return parentModel.bake(TRSRTransformation.from(this.rotation), format, bakedTextureGetter);
    }

    @Override
    public Collection<ResourceLocation> getDependencies()
    {
        return parentModel.getDependencies();
    }

    @Override
    public Collection<ResourceLocation> getTextures()
    {
        return parentModel.getTextures();
    }

    @Override
    public IModelState getDefaultState()
    {
        return parentModel.getDefaultState();
    }

    @Override
    public Optional<? extends IClip> getClip(String name)
    {
        return parentModel.getClip(name);
    }

    @Override
    public IModel process(ImmutableMap<String, String> customData)
    {
        return parentModel.process(customData);
    }

    @Override
    public IModel smoothLighting(boolean value)
    {
        return parentModel.smoothLighting(value);
    }

    @Override
    public IModel gui3d(boolean value)
    {
        return parentModel.gui3d(value);
    }

    @Override
    public IModel uvlock(boolean value)
    {
        return parentModel.uvlock(value);
    }

    @Override
    public IModel retexture(ImmutableMap<String, String> textures)
    {
        return parentModel.retexture(textures);
    }

    @Override
    public Optional<ModelBlock> asVanillaModel()
    {
        return parentModel.asVanillaModel();
    }
}
