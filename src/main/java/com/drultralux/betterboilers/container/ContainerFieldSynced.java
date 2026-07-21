package com.drultralux.betterboilers.container;

import com.drultralux.betterboilers.tile.IFieldProvider;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;

public abstract class ContainerFieldSynced extends Container {
    protected final IFieldProvider fieldProvider;
    private final int[] cachedFields;

    protected ContainerFieldSynced(IFieldProvider fieldProvider) {
        this.fieldProvider = fieldProvider;
        this.cachedFields = new int[fieldProvider.getFieldCount()];
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        for (IContainerListener listener : this.listeners) {
            for (int i = 0; i < fieldProvider.getFieldCount(); i++) {
                if (cachedFields[i] != fieldProvider.getField(i)) {
                    listener.sendWindowProperty(this, i, fieldProvider.getField(i));
                }
            }
        }
        for (int i = 0; i < fieldProvider.getFieldCount(); i++) {
            cachedFields[i] = fieldProvider.getField(i);
        }
    }

    @Override
    public void updateProgressBar(int id, int data) {
        fieldProvider.setField(id, data);
    }
}