package com.drultralux.betterboilers.network;

import com.drultralux.betterboilers.BBLog;
import com.drultralux.betterboilers.BetterBoilers;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.Map;

public class BBNetwork {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(BetterBoilers.MODID);

    public interface ServerPacketHandler {
        void handle(NBTTagCompound payload, EntityPlayerMP sender);
    }

    public interface ClientPacketHandler {
        void handle(NBTTagCompound payload);
    }

    private static final Map<String, ServerPacketHandler> SERVER_HANDLERS = new HashMap<>();
    private static final Map<String, ClientPacketHandler> CLIENT_HANDLERS = new HashMap<>();

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(BBPacket.ServerHandler.class, BBPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(BBPacket.ClientHandler.class, BBPacket.class, id++, Side.CLIENT);
    }

    public static void registerServerHandler(String type, ServerPacketHandler handler) {
        if (SERVER_HANDLERS.containsKey(type)) {
            throw new IllegalStateException("Duplicate server packet handler for type: " + type);
        }
        SERVER_HANDLERS.put(type, handler);
    }

    public static void registerClientHandler(String type, ClientPacketHandler handler) {
        if (CLIENT_HANDLERS.containsKey(type)) {
            throw new IllegalStateException("Duplicate client packet handler for type: " + type);
        }
        CLIENT_HANDLERS.put(type, handler);
    }

    static void dispatchServer(BBPacket msg, EntityPlayerMP sender) {
        ServerPacketHandler handler = SERVER_HANDLERS.get(msg.getType());
        if (handler != null) {
            handler.handle(msg.getPayload(), sender);
        } else {
            BBLog.warn("No server handler registered for packet type: " + msg.getType());
        }
    }

    static void dispatchClient(BBPacket msg) {
        ClientPacketHandler handler = CLIENT_HANDLERS.get(msg.getType());
        if (handler != null) {
            handler.handle(msg.getPayload());
        } else {
            BBLog.warn("No client handler registered for packet type: " + msg.getType());
        }
    }

    // --- convenience senders, mirroring SimpleNetworkWrapper's own send* methods ---

    public static void sendToServer(String type, NBTTagCompound payload) {
        CHANNEL.sendToServer(new BBPacket(type, payload));
    }

    public static void sendToPlayer(EntityPlayerMP player, String type, NBTTagCompound payload) {
        CHANNEL.sendTo(new BBPacket(type, payload), player);
    }

    public static void sendToAllTracking(TileEntity te, String type, NBTTagCompound payload) {
        if (te.getWorld() == null || te.getWorld().isRemote) return;
        WorldServer ws = (WorldServer) te.getWorld();
        Chunk chunk = ws.getChunkFromBlockCoords(te.getPos());
        BBPacket packet = new BBPacket(type, payload);
        for (EntityPlayer p : ws.playerEntities) {
            if (p instanceof EntityPlayerMP && ws.getPlayerChunkMap().isPlayerWatchingChunk((EntityPlayerMP) p, chunk.x, chunk.z)) {
                CHANNEL.sendTo(packet, (EntityPlayerMP) p);
            }
        }
    }

    public static void sendToAllAround(World world, BlockPos pos, double radius, String type, NBTTagCompound payload) {
        CHANNEL.sendToAllAround(new BBPacket(type, payload),
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), radius));
    }
}