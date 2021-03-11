package net.dries007.tfc.objects.blocks.property;

import com.google.common.base.Optional;

import net.minecraft.util.ResourceLocation;

import net.dries007.tfc.api.registries.TFCRegistries;
import net.dries007.tfc.api.types.Rock;
import zone.rong.zairyou.api.property.FreezableProperty;

public class RockProperty extends FreezableProperty<Rock>
{
    public RockProperty()
    {
        super("rock", Rock.class);
    }

    @Override
    public Optional<Rock> parseValue(String value)
    {
        return Optional.of(TFCRegistries.ROCKS.getValue(new ResourceLocation(value.replace("_", ":"))));
    }

    @Override
    public String getName(Rock value)
    {
        return value.getRegistryName().toString().replace(":", "_");
    }
}
