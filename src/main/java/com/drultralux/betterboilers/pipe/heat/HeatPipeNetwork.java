package com.drultralux.betterboilers.pipe.heat;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.pipe.ScalarPipeNetwork;
import com.drultralux.betterboilers.util.BBConfig;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;

/**
 * Fully functional transport layer today - it just has nothing to talk to until roadmap item 2
 * (the heat system) adds real IHeatHandler sources and sinks. All the actual transfer-loop logic
 * lives in PipeNetwork/ScalarPipeNetwork; this class only supplies the heat-specific glue.
 */
public class HeatPipeNetwork extends ScalarPipeNetwork<IHeatHandler> {

    public HeatPipeNetwork(World world) {
        super(world);
    }

    public int getHeatStored() {
        return (int) Math.min(Integer.MAX_VALUE, getStoredAmount());
    }

    public int insertHeat(int maxInsert, boolean simulate) {
        return (int) receive(maxInsert, simulate);
    }

    public int extractHeat(int maxExtract, boolean simulate) {
        return (int) extract(maxExtract, simulate);
    }

    @Override
    protected long getCapacityPerSegment() {
        return BBConfig.heatPipeCapacityPerSegment;
    }

    @Override
    protected int getMaxTransferPerTick() {
        return BBConfig.heatPipeMaxTransferPerTick;
    }

    @Override
    protected Capability<IHeatHandler> getCapabilityType() {
        return HeatCapability.HEAT;
    }

    protected long extractFromHandler(IHeatHandler handler, long maxAmount, boolean simulate) {
        if (!handler.canExtract()) {
            BBLog.debug("HeatPipeNetwork: source handler {} canExtract=false - skipping", handler.getClass().getSimpleName());
            return 0;
        }
        int capped = (int) Math.min(Integer.MAX_VALUE, maxAmount);
        long extracted = handler.extractHeat(capped, simulate);
        BBLog.debug("HeatPipeNetwork: extractFromHandler on {} - maxAmount={}, simulate={}, extracted={}",
                handler.getClass().getSimpleName(), maxAmount, simulate, extracted);
        return extracted;
    }

    @Override
    protected long insertToHandler(IHeatHandler handler, long maxAmount, boolean simulate) {
        if (!handler.canInsert()) {
            BBLog.debug("HeatPipeNetwork: sink handler {} canInsert=false - skipping", handler.getClass().getSimpleName());
            return 0;
        }
        int capped = (int) Math.min(Integer.MAX_VALUE, maxAmount);
        long inserted = handler.insertHeat(capped, simulate);
        BBLog.debug("HeatPipeNetwork: insertToHandler on {} - maxAmount={}, simulate={}, inserted={}",
                handler.getClass().getSimpleName(), maxAmount, simulate, inserted);
        return inserted;
    }
}
