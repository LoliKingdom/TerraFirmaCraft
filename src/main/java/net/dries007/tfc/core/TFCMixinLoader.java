package net.dries007.tfc.core;

import org.spongepowered.asm.mixin.Mixins;
import zone.rong.mixinbooter.MixinLoader;

@MixinLoader
public class TFCMixinLoader
{
    {
        Mixins.addConfiguration("mixins.tfc.json");
    }
}
