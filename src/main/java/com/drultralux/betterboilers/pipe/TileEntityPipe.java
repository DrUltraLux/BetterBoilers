package com.drultralux.betterboilers.pipe;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import java.util.function.Supplier;

/**
 * Shared base for every pipe tile entity (fluid, energy, heat). Handles network bookkeeping
 * and the re-entrancy guard; each concrete subclass supplies its own connectivity check,
 * network factory, and capability exposure.
 */
public abstract class TileEntityPipe extends TileEntity {

    private final boolean[] connected = new boolean[6];

    /**
     * Guards against re-entrant fill/drain/receiveEnergy calls while this pipe is already mid-transfer.
     * Never persisted - it only needs to hold for the duration of a single synchronous call chain.
     * This is the single node-level safeguard that would have stopped the IE/Gadgetry mutual-recursion
     * crash regardless of which mod was on the other end.
     */
    private transient boolean inTransfer = false;

    /** Assigned by PipeNetworkManager. Never persisted - networks are cheap to rebuild after a load. */
    private transient PipeNetwork<?> network = null;

    public abstract PipeType getPipeType();

    /** Construct a brand-new, empty network of the correct concrete type for this pipe. */
    public abstract PipeNetwork<?> createNetwork();

    /** True if a same-type pipe, or a tile exposing the relevant capability, sits on this side. */
    public abstract boolean canConnect(EnumFacing side);

    public PipeNetwork<?> getNetwork() {
        return network;
    }

    public void setNetwork(PipeNetwork<?> network) {
        this.network = network;
    }

    /** Attempt to enter a transfer section. Returns false if already mid-transfer (re-entrant call). */
    public boolean beginTransfer() {
        if (inTransfer) {
            return false;
        }
        inTransfer = true;
        return true;
    }

    public void endTransfer() {
        inTransfer = false;
    }

    /**
     * Java's type system cannot prove that a Forge capability-token equality check
     * (capability == SomeCapability.INSTANCE) implies T equals the concrete handler type - that
     * link only exists at runtime, once the token comparison has already passed. Capability.cast()
     * doesn't help here: it narrows a reference already known to be T down to some subtype R, not
     * the reverse (turning an arbitrary object we merely know satisfies T at runtime into T
     * itself). This is the single, deliberately isolated place in the whole pipe system where an
     * unchecked cast is unavoidable - every call site using it has already validated the
     * capability token and the handler's non-nullness before reaching this line.
     */
    @SuppressWarnings("unchecked")
    protected static <T> T castCapabilityHandler(Object handler) {
        return (T) handler;
    }

    /**
     * Runs action() guarded by this tile's re-entrancy flag, returning fallback if this tile is
     * already mid-transfer (a reactive re-entrant call). Collapses the repeated
     * beginTransfer()/try/finally-endTransfer() shape that every capability wrapper needs.
     */
    protected <R> R guarded(Supplier<R> action, R fallback) {
        if (!beginTransfer()) {
            return fallback;
        }
        try {
            return action.get();
        } finally {
            endTransfer();
        }
    }

    public boolean isConnected(EnumFacing side) {
        return connected[side.getIndex()];
    }

    public void setConnected(EnumFacing side, boolean value) {
        connected[side.getIndex()] = value;
    }

    /** Recompute which sides connect and update the blockstate/render accordingly. Server-side topology change only. */
    public void refreshConnections() {
        boolean changed = false;
        for (EnumFacing side : EnumFacing.VALUES) {
            boolean now = canConnect(side);
            if (now != isConnected(side)) {
                setConnected(side, now);
                changed = true;
            }
        }
        if (changed && world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public void markNetworkDirty() {
        if (world != null && !world.isRemote) {
            PipeNetworkManager.INSTANCE.markDirty(world, getPos(), getPipeType());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        markNetworkDirty();
    }

    public void onNeighborChanged() {
        refreshConnections();
        markNetworkDirty();
    }

    /**
     * Called by the owning Block's breakBlock, just before this tile is actually removed from the
     * world. Neighboring pipes are notified of the change via vanilla's own neighbor-update
     * mechanism once the block is gone, which is what re-derives correct network topology - this
     * hook only needs to drop our own dangling network reference.
     */
    public void onRemoved() {
        network = null;
    }
}
