/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package net.dries007.tfc.util.forge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;

@ParametersAreNonnullByDefault
public class ForgeSteps implements INBTSerializable<NBTTagCompound>
{
    private final byte[] steps;

    public ForgeSteps()
    {
        steps = new byte[4];
        reset();
    }

    public ForgeSteps(byte[] steps)
    {
        this.steps = steps.clone();
    }

    public void reset()
    {
        steps[3] = 0; // Reset pointer index to first
        for (int i = 0; i < 3; i++)
        {
            addStep(-1);
        }
    }

    public void addStep(@Nullable ForgeStep step)
    {
        byte pointer = steps[3];
        steps[pointer] = step == null ? -1 : (byte) step.ordinal();
        steps[3] = ++pointer > 3 ? 0 : pointer;
    }

    public void addStep(int step) {
        byte pointer = steps[3];
        steps[pointer] = (byte) step;
        steps[3] = ++pointer > 3 ? 0 : pointer;
    }

    @Override
    @Nonnull
    public NBTTagCompound serializeNBT()
    {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setByte("last", steps[0]);
        nbt.setByte("second", steps[1]);
        nbt.setByte("third", steps[2]);
        nbt.setByte("pointer", steps[3]);
        return nbt;
    }

    @Override
    public void deserializeNBT(@Nullable NBTTagCompound nbt)
    {
        if (nbt != null)
        {
            addStep(nbt.getByte("last"));
            addStep(nbt.getByte("second"));
            addStep(nbt.getByte("third"));
            addStep(nbt.getByte("pointer"));
        }
    }

    @Nonnull
    public ForgeSteps copy()
    {
        return new ForgeSteps(this.steps);
    }

    @Nullable
    public ForgeStep getStep(int idx)
    {
        return ForgeStep.valueOf(steps[idx]);
    }

    @Override
    public String toString()
    {
        return "[" + getStep(0) + ", " + getStep(1) + ", " + getStep(2) + "]";
    }

    /**
     * Checks if this is fresh new (no forging has been done yet)
     *
     * @return true if has been worked at least once, false otherwise
     */
    public boolean hasWork()
    {
        for (byte step : steps)
        {
            if (step != -1)
            {
                return true;
            }
        }
        return false;
    }
}
