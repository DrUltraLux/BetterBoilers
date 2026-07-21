package com.drultralux.betterboilers.tile;

public interface IFieldProvider {
    int getField(int id);
    void setField(int id, int value);
    int getFieldCount();
}
