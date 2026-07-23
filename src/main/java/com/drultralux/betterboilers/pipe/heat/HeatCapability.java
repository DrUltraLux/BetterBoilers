package com.drultralux.betterboilers.pipe.heat;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

public class HeatCapability {

    @CapabilityInject(IHeatHandler.class)
    public static Capability<IHeatHandler> HEAT = null;

    private HeatCapability() {}

    public static void register() {
        CapabilityManager.INSTANCE.register(IHeatHandler.class,
                new Capability.IStorage<IHeatHandler>() {
                    @Override
                    public NBTBase writeNBT(Capability<IHeatHandler> capability, IHeatHandler instance, EnumFacing side) {
                        return new NBTTagInt(instance.getHeatStored());
                    }

                    @Override
                    public void readNBT(Capability<IHeatHandler> capability, IHeatHandler instance, EnumFacing side, NBTBase nbt) {
                        if (instance instanceof HeatStorage && nbt instanceof NBTTagInt) {
                            ((HeatStorage) instance).heat = ((NBTTagInt) nbt).getInt();
                        }
                    }
                },
                () -> new HeatStorage(0));
    }
}
