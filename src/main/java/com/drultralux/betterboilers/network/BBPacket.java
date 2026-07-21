package com.drultralux.betterboilers.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Single generic network envelope for the whole mod. `type` identifies which subsystem
 * should handle the payload; `payload` carries whatever NBT that subsystem needs.
 * New features register a handler with BBNetwork instead of writing a new IMessage.
 */
public class BBPacket implements IMessage {
    private String type = "";
    private NBTTagCompound payload = new NBTTagCompound();

    public BBPacket() {}

    public BBPacket(String type, NBTTagCompound payload) {
        this.type = type;
        this.payload = payload != null ? payload : new NBTTagCompound();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        type = ByteBufUtils.readUTF8String(buf);
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        payload = tag != null ? tag : new NBTTagCompound();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, type);
        ByteBufUtils.writeTag(buf, payload);
    }

    public String getType() { return type; }
    public NBTTagCompound getPayload() { return payload; }

    public static class ServerHandler implements IMessageHandler<BBPacket, IMessage> {
        @Override
        public IMessage onMessage(BBPacket message, MessageContext ctx) {
            EntityPlayerMP sender = ctx.getServerHandler().player;
            // Network handlers run off the main thread; hop back on before touching world/TEs.
            sender.getServerWorld().addScheduledTask(() -> BBNetwork.dispatchServer(message, sender));
            return null;
        }
    }

    public static class ClientHandler implements IMessageHandler<BBPacket, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(BBPacket message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> BBNetwork.dispatchClient(message));
            return null;
        }
    }
}