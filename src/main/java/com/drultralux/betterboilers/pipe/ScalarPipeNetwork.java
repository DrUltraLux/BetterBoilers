package com.drultralux.betterboilers.pipe;

import com.drultralux.betterboilers.BBLog;
import net.minecraft.world.World;

import java.util.List;

/**
 * Base for pipe networks whose pooled resource is a single plain number with no type to match
 * (RF, heat - unlike fluid, which also has to track which fluid is currently in the pipe).
 * Energy and heat networks were previously near-identical copies of each other; this is that
 * shared shape, factored out once.
 */
public abstract class ScalarPipeNetwork<H> extends PipeNetwork<H> {

    protected long amount = 0;

    protected ScalarPipeNetwork(World world) {
        super(world);
    }

    @Override
    public long getStoredAmount() {
        return amount;
    }

    /** Reactive entry point for an external tile pushing resource directly into this network. */
    public long receive(long maxReceive, boolean simulate) {
        if (maxReceive < 0) {
            BBLog.warn("{} received a negative receive amount ({}) - treating as 0", getClass().getSimpleName(), maxReceive);
            return 0;
        }
        long room = Math.max(0, getCapacity() - amount);
        long accepted = Math.min(room, maxReceive);
        if (accepted > 0 && !simulate) {
            amount += accepted;
        }
        return accepted;
    }

    /** Reactive entry point for an external tile pulling resource directly out of this network. */
    public long extract(long maxExtract, boolean simulate) {
        if (maxExtract < 0) {
            BBLog.warn("{} received a negative extract amount ({}) - treating as 0", getClass().getSimpleName(), maxExtract);
            return 0;
        }
        long removed = Math.min(amount, maxExtract);
        if (removed > 0 && !simulate) {
            amount -= removed;
        }
        return removed;
    }

    @Override
    public void mergeFrom(List<PipeNetwork<?>> oldNetworks) {
        long total = 0;
        for (PipeNetwork<?> old : oldNetworks) {
            if (old instanceof ScalarPipeNetwork) {
                total += ((ScalarPipeNetwork<?>) old).amount;
            } else {
                BBLog.warn("{} merge encountered an incompatible network type ({}) - its contents were discarded",
                        getClass().getSimpleName(), old.getClass().getSimpleName());
            }
        }
        long cap = getCapacity();
        if (total > cap) {
            BBLog.debug("{} merge produced {} which exceeds new capacity {} - clamping", getClass().getSimpleName(), total, cap);
        }
        this.amount = Math.min(total, cap);
    }

    @Override
    protected int pullFrom(H source, int maxAmount) {
        long extracted = extractFromHandler(source, maxAmount, true);
        if (extracted <= 0) {
            return 0;
        }
        long actual = extractFromHandler(source, extracted, false);
        if (actual > 0) {
            amount += actual;
        }
        return (int) Math.min(Integer.MAX_VALUE, actual);
    }

    @Override
    protected int pushTo(H sink, int maxAmount) {
        long accepted = insertToHandler(sink, maxAmount, true);
        if (accepted <= 0) {
            return 0;
        }
        long actual = insertToHandler(sink, accepted, false);
        if (actual > 0) {
            amount -= actual;
        }
        return (int) Math.min(Integer.MAX_VALUE, actual);
    }

    /** Call the resource-specific extract method on an external handler (e.g. IEnergyStorage.extractEnergy). */
    protected abstract long extractFromHandler(H handler, long maxAmount, boolean simulate);

    /** Call the resource-specific insert method on an external handler (e.g. IEnergyStorage.receiveEnergy). */
    protected abstract long insertToHandler(H handler, long maxAmount, boolean simulate);
}
