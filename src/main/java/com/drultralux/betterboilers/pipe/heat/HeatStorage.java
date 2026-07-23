package com.drultralux.betterboilers.pipe.heat;

public class HeatStorage implements IHeatHandler {

    protected int heat;
    protected int capacity;
    protected int maxInsert;
    protected int maxExtract;

    public HeatStorage(int capacity) {
        this(capacity, capacity, capacity, 0);
    }

    public HeatStorage(int capacity, int maxInsert, int maxExtract, int heat) {
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.heat = Math.max(0, Math.min(capacity, heat));
    }

    /**
     * Resizes this storage in place, mirroring FluidTank.setCapacity - used by multiblock
     * controllers whose capacity depends on block count and can change on every rebuild. Existing
     * stored heat is clamped down rather than lost outright if the new capacity is smaller.
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.maxInsert = capacity;
        this.maxExtract = capacity;
        if (this.heat > capacity) {
            this.heat = capacity;
        }
    }

    /**
     * Sets the maximum heat this storage can accept in a single insertHeat() call, independent of
     * total capacity - e.g. a tank controller's per-tick acceptance rate should scale with its
     * block count, not with how much it can hold in total.
     */
    public void setMaxInsert(int maxInsert) {
        this.maxInsert = maxInsert;
    }

    /**
     * Sets the maximum heat this storage can give up in a single extractHeat() call, independent
     * of total capacity - e.g. a furnace controller's per-tick export rate should scale with its
     * block count, not with how much it can hold in total.
     */
    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
    }

    /**
     * Directly increases stored heat, bypassing maxInsert, returning the amount actually added
     * (may be less than requested if near capacity). A tile's own internal generation (e.g. a
     * furnace controller burning fuel, or a heat sink passively drawing off adjacent lava/magma)
     * is a different channel from external capability-driven insertion, and should never be
     * limited by the same throughput cap that keeps an external pipe from pushing heat in - both
     * typically want maxInsert set low or to 0 to refuse that, while still generating freely.
     */
    public int generateHeat(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = heat;
        heat = Math.min(capacity, heat + amount);
        return heat - before;
    }

    /**
     * Directly decreases stored heat, bypassing maxExtract, returning the amount actually removed
     * (may be less than requested if not enough is stored). A controller's own internal
     * consumption (e.g. a tank converting its own stored heat into steam) is a different channel
     * from external capability-driven extraction, and should never be limited by the same
     * throughput cap that keeps an external pipe from pulling heat back out - a tank typically
     * wants maxExtract = 0 to refuse that, while still consuming its own heat freely internally.
     */
    public int consumeHeat(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = heat;
        heat = Math.max(0, heat - amount);
        return before - heat;
    }

    @Override
    public int insertHeat(int maxInsert, boolean simulate) {
        int accepted = Math.min(capacity - heat, Math.min(this.maxInsert, maxInsert));
        if (accepted <= 0) {
            return 0;
        }
        if (!simulate) {
            heat += accepted;
        }
        return accepted;
    }

    @Override
    public int extractHeat(int maxExtract, boolean simulate) {
        int removed = Math.min(heat, Math.min(this.maxExtract, maxExtract));
        if (removed <= 0) {
            return 0;
        }
        if (!simulate) {
            heat -= removed;
        }
        return removed;
    }

    @Override
    public int getHeatStored() {
        return heat;
    }

    @Override
    public int getMaxHeatStored() {
        return capacity;
    }

    @Override
    public boolean canInsert() {
        return maxInsert > 0;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }
}
