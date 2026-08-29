package com.moze.openmmdchanged.network;

import com.moze.openmmdchanged.player.WaterFloatController;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record WaterFloatInputPacket(boolean held) {
    static void encode(WaterFloatInputPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.held);
    }

    static WaterFloatInputPacket decode(FriendlyByteBuf buffer) {
        return new WaterFloatInputPacket(buffer.readBoolean());
    }

    static void handle(WaterFloatInputPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getSender() != null) {
                WaterFloatController.setSyncedJumpInput(context.getSender(), packet.held);
            }
        });
        context.setPacketHandled(true);
    }
}
