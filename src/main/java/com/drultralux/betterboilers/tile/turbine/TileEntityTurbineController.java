package com.drultralux.betterboilers.tile.turbine;

import com.drultralux.betterboilers.block.turbine.BlockTurbineController;
import com.drultralux.betterboilers.block.ModBlocks;
import com.drultralux.betterboilers.network.BBNetwork;
import com.drultralux.betterboilers.tile.IControlledPart;
import com.drultralux.betterboilers.tile.IFieldProvider;
import com.drultralux.betterboilers.tile.TileEntityMultiblockController;
import com.drultralux.betterboilers.util.BBConfig;
import com.drultralux.betterboilers.util.ObservableEnergyStorage;
import com.drultralux.betterboilers.BBLog;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileEntityTurbineController extends TileEntityMultiblockController implements ITickable, IFieldProvider, IControlledPart<TileEntityTurbineController> {

    public FluidTank tankSteam;
    public ObservableEnergyStorage energyStorage;

    private List<TurbineStage> currentStages = new ArrayList<>();
    private List<BlockPos> currentAirBlocks = new ArrayList<>();
    private boolean structureValid = false;
    private double totalCaptureCapacity = 0;
    private List<BlockPos> previousControlledPositions = new ArrayList<>();
    private List<BlockPos> scanControlledPositions;

    private boolean scanning = false;
    private EnumFacing scanShaftAxis;
    private BlockPos scanCursor;
    private int scanWalked;
    private List<TurbineStage> scanStages;
    private List<BlockPos> scanAirBlocks;

    private static class TurbineInvalidException extends RuntimeException {
        final String reasonKey;
        TurbineInvalidException(String reasonKey) { this.reasonKey = reasonKey; }
    }

    public TileEntityTurbineController getController() {
        return this;
    }

    public void setController(TileEntityTurbineController controller) {
    }

    public TileEntityTurbineController() {
        this.tankSteam = new FluidTank(0) {
            @Override
            public boolean canFillFluidType(FluidStack fluid) {
                return fluid != null && fluid.getFluid() == ModBlocks.FLUID_STEAM;
            }

            @Override
            public int fill(FluidStack resource, boolean doFill) {
                int filled = super.fill(resource, doFill);
                if (doFill && filled > 0) markDirty();
                return filled;
            }

            @Override
            public FluidStack drain(int maxDrain, boolean doDrain) {
                FluidStack drained = super.drain(maxDrain, doDrain);
                if (doDrain && drained != null) markDirty();
                return drained;
            }
        };
        this.energyStorage = new ObservableEnergyStorage(100_000, 0, BBConfig.turbineOut);
        energyStorage.listen(this::markDirty);
    }

    @Override
    public void update() {
        if (world.isRemote) return;

        energyStorage.tick();
        tickTurbineScan();

        if (structureValid) {
            processGeneration();
        }
    }

    @Override
    public void onAssemble(World world, List<BlockPos> blocks) {
        // Not used - see tickTurbineScan()/finishTurbineScan(), which handle part-controller
        // assignment directly for the turbine's directed-walk algorithm.
    }

    @Override
    public void onDisassemble(World world, List<BlockPos> blocks) {
        // Not used - see startTurbineScan(), which clears stale controller references
        // from previousArmPositions at the start of every scan.
    }

    private void tickTurbineScan() {
        try {
            if (!scanning) {
                if (!consumeRescanTrigger()) return;
                startTurbineScan();
            }
            if (scanning) {
                stepTurbineScan();
            }
        } catch (TurbineInvalidException e) {
            applyFailure(e.reasonKey);
        }
    }

    private void startTurbineScan() {
        for (BlockPos p : previousControlledPositions) {
            TileEntity te = world.getTileEntity(p);
            if (te instanceof TileEntityTurbinePart) {
                ((TileEntityTurbinePart) te).setController(null);
            }
        }
        previousControlledPositions = new ArrayList<>();

        EnumFacing axis = findShaftAxis();
        if (axis == null) {
            BBLog.debug("Turbine {} : no valid shaft axis (need exactly one adjacent Shaft block)", getPos());
            throw new TurbineInvalidException("msg.bb.badTurbineController");
        }
        BBLog.debug("Turbine {} : starting scan, shaft axis = {}", getPos(), axis);
        scanShaftAxis = axis;
        scanCursor = getPos().offset(axis);
        scanWalked = 0;
        scanStages = new ArrayList<>();
        scanAirBlocks = new ArrayList<>();
        scanControlledPositions = new ArrayList<>();

        for (EnumFacing ef : EnumFacing.VALUES) {
            if (ef == axis) continue;
            BlockPos p = getPos().offset(ef);
            if (world.getBlockState(p).getBlock() == ModBlocks.PRESSURE_VALVE) {
                BBLog.debug("Turbine {} : found Pressure Valve at {}", getPos(), p);
                registerControlledPart(p);
            }
        }

        scanning = true;
    }

    private void registerControlledPart(BlockPos p) {
        TileEntity te = world.getTileEntity(p);
        if (te instanceof TileEntityTurbinePart) {
            ((TileEntityTurbinePart) te).setController(this);
        }
        scanControlledPositions.add(p);
    }

    private void stepTurbineScan() {
        Block block = world.getBlockState(scanCursor).getBlock();
        BBLog.debug("Turbine {} : step {} at {} - block = {}", getPos(), scanWalked, scanCursor, block.getRegistryName());

        if (block == ModBlocks.POWER_TAP) {
            BBLog.debug("Turbine {} : Power Tap found at {}, finishing scan", getPos(), scanCursor);
            registerControlledPart(scanCursor);
            finishTurbineScan();
            return;
        }
        if (block != ModBlocks.SHAFT) {
            BBLog.debug("Turbine {} : expected Rotor or Power Tap at {} but found {} instead", getPos(), scanCursor, block.getRegistryName());
            throw new TurbineInvalidException("msg.bb.badTurbinePowerTap");
        }

        registerControlledPart(scanCursor);

        scanWalked++;
        if (scanWalked > BBConfig.maxTurbineShaftLength) {
            BBLog.debug("Turbine {} : shaft exceeded max length ({})", getPos(), BBConfig.maxTurbineShaftLength);
            throw new TurbineInvalidException("msg.bb.turbineTooLong");
        }

        processSlice(scanCursor);
        scanCursor = scanCursor.offset(scanShaftAxis);
    }

    private void finishTurbineScan() {
        scanning = false;
        if (scanStages.isEmpty()) {
            BBLog.debug("Turbine {} : scan completed with zero valid rotor stages", getPos());
            throw new TurbineInvalidException("msg.bb.noTurbineStages");
        }

        for (TurbineStage stage : scanStages) {
            for (BlockPos p : stage.armPositions()) {
                registerControlledPart(p);
            }
        }
        previousControlledPositions = scanControlledPositions;

        this.currentStages = scanStages;
        double captureSum = 0;
        for (TurbineStage stage : scanStages) captureSum += stage.captureCapacity();
        this.totalCaptureCapacity = captureSum;

        this.currentAirBlocks = scanAirBlocks;
        tankSteam.setCapacity(scanAirBlocks.size() * BBConfig.mBPerOpenTurbineSlot);

        BBLog.debug("Turbine {} : scan SUCCESS - {} stage(s), totalCapture={}, airBlocks={}, tankCapacityMb={}",
                getPos(), scanStages.size(), totalCaptureCapacity, scanAirBlocks.size(), scanAirBlocks.size() * BBConfig.mBPerOpenTurbineSlot);

        this.structureValid = true;
        setControllerStatus(ControllerStatus.ACTIVE, "msg.bb.noIssue");
        markDirty();
    }

    private void applyFailure(String reasonKey) {
        BBLog.debug("Turbine {} : scan FAILED - reason = {}", getPos(), reasonKey);
        scanning = false;
        structureValid = false;
        currentStages = new ArrayList<>();
        currentAirBlocks = new ArrayList<>();
        totalCaptureCapacity = 0;
        setControllerStatus(ControllerStatus.ERRORED, reasonKey);
    }

    private EnumFacing findShaftAxis() {
        EnumFacing found = null;
        for (EnumFacing ef : EnumFacing.VALUES) {
            if (world.getBlockState(getPos().offset(ef)).getBlock() == ModBlocks.SHAFT) {
                if (found != null) return null;
                found = ef;
            }
        }
        return found;
    }

    private void processSlice(BlockPos shaftPos) {
        List<EnumFacing> perpDirs = new ArrayList<>();
        for (EnumFacing ef : EnumFacing.VALUES) {
            if (ef.getAxis() != scanShaftAxis.getAxis()) perpDirs.add(ef);
        }

        List<EnumFacing> bladeDirs = new ArrayList<>();
        for (EnumFacing ef : perpDirs) {
            if (world.getBlockState(shaftPos.offset(ef)).getBlock() == ModBlocks.BLADE) {
                bladeDirs.add(ef);
            }
        }
        BBLog.debug("Turbine {} : slice at {} - bladeDirs = {}", getPos(), shaftPos, bladeDirs);

        if (!bladeDirs.isEmpty()) {
            boolean validPattern;
            if (bladeDirs.size() == 4) validPattern = true;
            else if (bladeDirs.size() == 2) validPattern = bladeDirs.get(0).getOpposite() == bladeDirs.get(1);
            else validPattern = false;
            if (!validPattern) {
                BBLog.debug("Turbine {} : slice at {} - invalid blade pattern ({} direction(s))", getPos(), shaftPos, bladeDirs.size());
                throw new TurbineInvalidException("msg.bb.badTurbineBlades");
            }

            int armLength = -1;
            List<List<BlockPos>> arms = new ArrayList<>();
            for (EnumFacing dir : bladeDirs) {
                List<BlockPos> arm = new ArrayList<>();
                BlockPos p = shaftPos.offset(dir);
                while (world.getBlockState(p).getBlock() == ModBlocks.BLADE) {
                    arm.add(p);
                    p = p.offset(dir);
                }
                if (arm.size() > BBConfig.maxTurbineArmLength) {
                    BBLog.debug("Turbine {} : slice at {} - arm toward {} too long ({} > {})", getPos(), shaftPos, dir, arm.size(), BBConfig.maxTurbineArmLength);
                    throw new TurbineInvalidException("msg.bb.turbineArmTooLong");
                }
                if (armLength == -1) armLength = arm.size();
                else if (arm.size() != armLength) {
                    BBLog.debug("Turbine {} : slice at {} - mismatched arm toward {} (length {} vs {})", getPos(), shaftPos, dir, arm.size(), armLength);
                    throw new TurbineInvalidException("msg.bb.mismatchedTurbineArms");
                }
                arms.add(arm);
            }

            List<BlockPos> allArmPositions = new ArrayList<>();
            for (List<BlockPos> arm : arms) allArmPositions.addAll(arm);

            CoilTier tier;
            int validArmCount;
            if (bladeDirs.size() == 4) {
                tier = validateSquareCoilRing(shaftPos, bladeDirs, armLength);
                validArmCount = (tier != null) ? 4 : 0;
            } else {
                BlockPos tipA = arms.get(0).get(armLength - 1).offset(bladeDirs.get(0));
                BlockPos tipB = arms.get(1).get(armLength - 1).offset(bladeDirs.get(1));
                CoilTier a = CoilTier.fromBlock(world.getBlockState(tipA).getBlock());
                CoilTier b = CoilTier.fromBlock(world.getBlockState(tipB).getBlock());
                if (a != null && a == b) { tier = a; validArmCount = 2; }
                else if (a != null) { tier = a; validArmCount = 1; }
                else if (b != null) { tier = b; validArmCount = 1; }
                else { tier = null; validArmCount = 0; }
            }

            BBLog.debug("Turbine {} : slice at {} - armLength={}, bladeCount={}, coilTier={}, validArmCount={}",
                    getPos(), shaftPos, armLength, bladeDirs.size(), tier, validArmCount);

            scanStages.add(new TurbineStage(bladeDirs.size(), armLength, tier, validArmCount, allArmPositions));
        }

        List<BlockPos> sliceAir = floodFillSliceAir(shaftPos, perpDirs, bladeDirs);
        BBLog.debug("Turbine {} : slice at {} - air blocks found = {}", getPos(), shaftPos, sliceAir.size());
        scanAirBlocks.addAll(sliceAir);
    }

    private CoilTier validateSquareCoilRing(BlockPos shaftPos, List<EnumFacing> bladeDirs, int armLength) {
        List<EnumFacing> axisADirs = new ArrayList<>();
        List<EnumFacing> axisBDirs = new ArrayList<>();
        EnumFacing.Axis firstAxis = bladeDirs.get(0).getAxis();
        for (EnumFacing ef : bladeDirs) {
            (ef.getAxis() == firstAxis ? axisADirs : axisBDirs).add(ef);
        }

        int ringDist = armLength + 1;
        CoilTier tier = null;

        for (EnumFacing sideDir : axisADirs) {
            for (int k = -armLength; k <= armLength; k++) {
                EnumFacing crossDir = (k >= 0) ? axisBDirs.get(0) : axisBDirs.get(1);
                BlockPos p = shaftPos.offset(sideDir, ringDist).offset(crossDir, Math.abs(k));
                CoilTier t = CoilTier.fromBlock(world.getBlockState(p).getBlock());
                if (t == null) {
                    BBLog.debug("Turbine {} : coil ring check failed at {} - found {} instead of a coil", getPos(), p, world.getBlockState(p).getBlock().getRegistryName());
                    return null;
                }
                if (tier == null) tier = t; else if (t != tier) {
                    BBLog.debug("Turbine {} : coil ring tier mismatch at {} - expected {}, found {}", getPos(), p, tier, t);
                    return null;
                }
            }
        }
        for (EnumFacing sideDir : axisBDirs) {
            for (int k = -armLength; k <= armLength; k++) {
                EnumFacing crossDir = (k >= 0) ? axisADirs.get(0) : axisADirs.get(1);
                BlockPos p = shaftPos.offset(sideDir, ringDist).offset(crossDir, Math.abs(k));
                CoilTier t = CoilTier.fromBlock(world.getBlockState(p).getBlock());
                if (t == null) {
                    BBLog.debug("Turbine {} : coil ring check failed at {} - found {} instead of a coil", getPos(), p, world.getBlockState(p).getBlock().getRegistryName());
                    return null;
                }
                if (tier == null) tier = t; else if (t != tier) {
                    BBLog.debug("Turbine {} : coil ring tier mismatch at {} - expected {}, found {}", getPos(), p, tier, t);
                    return null;
                }
            }
        }
        return tier;
    }

    private List<BlockPos> floodFillSliceAir(BlockPos shaftPos, List<EnumFacing> perpDirs, List<EnumFacing> bladeDirs) {
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> collected = new ArrayList<>();

        List<BlockPos> seeds = new ArrayList<>();
        for (EnumFacing dir : perpDirs) {
            if (!bladeDirs.contains(dir)) {
                seeds.add(shaftPos.offset(dir));
            }
        }
        List<EnumFacing> axisADirs = new ArrayList<>();
        List<EnumFacing> axisBDirs = new ArrayList<>();
        EnumFacing.Axis firstAxis = perpDirs.get(0).getAxis();
        for (EnumFacing ef : perpDirs) {
            (ef.getAxis() == firstAxis ? axisADirs : axisBDirs).add(ef);
        }
        for (EnumFacing a : axisADirs) {
            for (EnumFacing b : axisBDirs) {
                seeds.add(shaftPos.offset(a).offset(b));
            }
        }

        for (BlockPos seed : seeds) {
            if (seen.contains(seed)) continue;
            if (isFluidContamination(seed)) {
                throw new TurbineInvalidException("msg.bb.turbineFluidContamination");
            }
            if (!world.isAirBlock(seed)) { seen.add(seed); continue; }

            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(seed);
            seen.add(seed);
            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                collected.add(p);
                if (collected.size() > BBConfig.maxTurbineSliceAirBlocks) {
                    throw new TurbineInvalidException("msg.bb.turbineSliceUnsealed");
                }
                for (EnumFacing dir : perpDirs) {
                    BlockPos n = p.offset(dir);
                    if (seen.contains(n)) continue;
                    seen.add(n);
                    if (isFluidContamination(n)) {
                        throw new TurbineInvalidException("msg.bb.turbineFluidContamination");
                    }
                    if (world.isAirBlock(n)) {
                        queue.add(n);
                    }
                }
            }
        }
        return collected;
    }

    private boolean isFluidContamination(BlockPos pos) {
        Material mat = world.getBlockState(pos).getMaterial();
        boolean contaminated = mat == Material.WATER || mat == Material.LAVA;
        if (contaminated) {
            BBLog.debug("Turbine {} : fluid contamination at {}", getPos(), pos);
        }
        return contaminated;
    }

    private void processGeneration() {
        int available = tankSteam.getFluidAmount();
        if (available <= 0 || totalCaptureCapacity <= 0 || currentStages.isEmpty()) return;

        double capacitySum = 0;
        double weightedEfficiencySum = 0;
        for (TurbineStage stage : currentStages) {
            if (stage.tier == null || stage.validArmCount <= 0) continue;
            double stageCapacity = stage.captureCapacity();
            weightedEfficiencySum += stageCapacity * stage.tier.efficiency();
            capacitySum += stageCapacity;
        }
        if (capacitySum <= 0) return;
        double blendedEfficiency = weightedEfficiencySum / capacitySum;

        double effectiveFlow = Math.min(available, totalCaptureCapacity);
        int steamToDrain = (int) Math.round(effectiveFlow);
        if (steamToDrain <= 0) return;

        FluidStack drained = tankSteam.drain(steamToDrain, true);
        if (drained == null) return;

        int fePerTick = (int) Math.round(drained.amount * blendedEfficiency);
        energyStorage.generateEnergy(fePerTick, false);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        NBTTagCompound tag = super.writeToNBT(compound);
        tag.setInteger("SteamTankCapacity", tankSteam.getCapacity());
        tag.setTag("SteamTank", tankSteam.writeToNBT(new NBTTagCompound()));
        NBTBase energyTag = CapabilityEnergy.ENERGY.getStorage().writeNBT(CapabilityEnergy.ENERGY, energyStorage, null);
        tag.setTag("energy", energyTag);
        tag.setFloat("EnergyPerTick", energyStorage.getCurTransfer());
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        tankSteam.setCapacity(compound.getInteger("SteamTankCapacity"));
        tankSteam.readFromNBT(compound.getCompoundTag("SteamTank"));
        NBTBase energyTag = compound.getTag("energy");
        if (energyTag != null) {
            try {
                CapabilityEnergy.ENERGY.getStorage().readNBT(CapabilityEnergy.ENERGY, energyStorage, null, energyTag);
            } catch (Throwable t) {}
        }
        energyStorage.setCurTransfer(compound.getFloat("EnergyPerTick"));
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {
        readFromNBT(tag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public void markDirty() {
        super.markDirty();
        if (!hasWorld() || getWorld().isRemote) return;
        WorldServer ws = (WorldServer) getWorld();
        Chunk c = getWorld().getChunkFromBlockCoords(getPos());
        SPacketUpdateTileEntity packet = new SPacketUpdateTileEntity(getPos(), 0, getUpdateTag());
        for (EntityPlayerMP player : getWorld().getPlayers(EntityPlayerMP.class, Predicates.alwaysTrue())) {
            if (ws.getPlayerChunkMap().isPlayerWatchingChunk(player, c.x, c.z)) {
                player.connection.sendPacket(packet);
            }
        }
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        if (oldState.getBlock() == newState.getBlock()) return false;
        else return super.shouldRefresh(world, pos, oldState, newState);
    }

    public int getField(int id) {
        switch (id) {
            case 0: return energyStorage.getEnergyStored();
            case 1: return energyStorage.getMaxEnergyStored();
            case 2: return (int) (energyStorage.getCurTransfer() * 100);
            default: return 0;
        }
    }

    public void setField(int id, int value) {
        // server-authoritative; client receives via sync, doesn't set back
    }

    public int getFieldCount() {
        return 3;
    }

    public FluidTank getTankSteam() {
        return tankSteam;
    }

    public ObservableEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    public List<BlockPos> getCurrentAirBlocks() {
        return currentAirBlocks;
    }

    public void setControllerStatus(ControllerStatus state, String status) {
        errorReason = new TextComponentTranslation(status);
        if (state == ControllerStatus.ERRORED) {
            world.setBlockState(this.getPos(), ModBlocks.TURBINE_CONTROLLER.getDefaultState().withProperty(BlockTurbineController.ACTIVE, false));
        } else {
            world.setBlockState(this.getPos(), ModBlocks.TURBINE_CONTROLLER.getDefaultState().withProperty(BlockTurbineController.ACTIVE, true));
        }

        if (!status.equals(lastSentStatusKey)) {
            lastSentStatusKey = status;
            NBTTagCompound payload = new NBTTagCompound();
            payload.setInteger("x", getPos().getX());
            payload.setInteger("y", getPos().getY());
            payload.setInteger("z", getPos().getZ());
            payload.setString("key", status);
            BBNetwork.sendToAllTracking(this, "controller_status", payload);
        }
    }
}