package com.drultralux.betterboilers.tile.heat;

import com.drultralux.betterboilers.block.heat.BlockHeatSink;
import com.drultralux.betterboilers.pipe.heat.HeatCapability;
import com.drultralux.betterboilers.pipe.heat.HeatStorage;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Standalone (non-multiblock) tile. Checks all 6 faces every tick for a valid natural heat
 * source (Magma Block, Lava source block - not flowing lava) and sums heat generation across
 * however many of those faces are present, buffering it for a heat pipe to extract. maxInsert is
 * fixed at 0 so a pipe can't reactively push heat backward into this - see
 * HeatStorage.generateHeat() for how this tile still fills the buffer despite that.
 */
public class TileEntityHeatSink extends TileEntity implements ITickable {

    private HeatStorage heat = new HeatStorage(BBConfig.heatSinkCapacity, 0, BBConfig.heatSinkCapacity, 0);
    private boolean active = false;
    private int activeGraceTicksRemaining = 0;

    @Override
    public void update() {
        if (world.isRemote) {
            return;
        }

        int validFaces = 0;
        for (EnumFacing side : EnumFacing.VALUES) {
            if (isValidHeatSource(world.getBlockState(getPos().offset(side)))) {
                validFaces++;
            }
        }

        int generated = validFaces * BBConfig.heatPerSourceFaceTick;
        if (generated > 0) {
            heat.generateHeat(generated);
            markDirty();
            activeGraceTicksRemaining = BBConfig.heatSinkActiveGraceTicks;
        } else if (activeGraceTicksRemaining > 0) {
            activeGraceTicksRemaining--;
        }

        boolean nowActive = activeGraceTicksRemaining > 0;
        if (nowActive != active) {
            active = nowActive;
            world.setBlockState(getPos(), world.getBlockState(getPos()).withProperty(BlockHeatSink.ACTIVE, active));
        }
    }

    private boolean isValidHeatSource(IBlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.MAGMA) {
            return true;
        }
        if (block == Blocks.LAVA) {
            return state.getValue(BlockLiquid.LEVEL) == 0; // 0 = source block, not flowing
        }
        return false;
    }

    public HeatStorage getHeat() {
        return heat;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound tag = super.writeToNBT(compound);
        tag.setInteger("HeatStored", heat.getHeatStored());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        int stored = compound.getInteger("HeatStored");
        heat = new HeatStorage(BBConfig.heatSinkCapacity, 0, BBConfig.heatSinkCapacity, stored);
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (HeatCapability.HEAT != null && capability == HeatCapability.HEAT) {
            return castCapabilityHandler(heat);
        }
        return super.getCapability(capability, facing);
    }

    /**
     * Java's type system cannot prove that a Forge capability-token equality check
     * (capability == SomeCapability.INSTANCE) implies T equals the concrete handler type - that
     * link only exists at runtime, once the token comparison has already passed. This tile isn't
     * part of either multiblock hierarchy (it's a standalone TileEntity), so it can't reach the
     * equivalent helpers on TileEntityMultiblockController/TileEntityMultiblockPart - this is the
     * single, deliberately isolated place here where an unchecked cast is unavoidable.
     */
    @SuppressWarnings("unchecked")
    private static <T> T castCapabilityHandler(Object handler) {
        return (T) handler;
    }
}