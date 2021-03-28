package net.dries007.tfc.core.mixins.experimental;

import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;

import biomesoplenty.common.world.WorldTypeBOP;
import net.dries007.tfc.world.classic.worldgen.experimental.NewExperimentalGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldTypeBOP.class)
public class WorldTypeBOPMixin
{

    @Inject(method = "getChunkGenerator", at = @At("HEAD"), cancellable = true)
    private void getExperimentalGenerator(World world, String generatorOptions, CallbackInfoReturnable<IChunkGenerator> cir)
    {
        cir.setReturnValue(new NewExperimentalGenerator(world, generatorOptions));
    }

}
