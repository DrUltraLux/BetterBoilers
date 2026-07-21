package com.drultralux.betterboilers.client.framework;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import java.util.HashMap;
import java.util.Map;

public class BBGuiHandler implements IGuiHandler {
    private static final Map<String, Integer> NAMES = new HashMap<>();
    private static final Map<Integer, IGuiFactory> FACTORIES = new HashMap<>();
    private static int nextId = 0;

    public static void registerGui(IGuiFactory factory) {
        int id = nextId++;
        NAMES.put(factory.getName(), id);
        FACTORIES.put(id, factory);
    }

    public static int getGuiId(String name) {
        return NAMES.get(name);
    }

    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        IGuiFactory factory = FACTORIES.get(ID);
        return factory != null ? factory.constructContainer(player, world, x, y, z) : null;
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        IGuiFactory factory = FACTORIES.get(ID);
        return factory != null ? factory.constructGui(player, world, x, y, z) : null;
    }
}
