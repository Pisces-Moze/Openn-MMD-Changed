package com.moze.openmmdchanged.network;

import com.moze.openmmdchanged.OpenMmdChanged;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            OpenMmdChanged.id("network"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);
    private static boolean initialized;

    private ModNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        CHANNEL.registerMessage(0, WaterFloatInputPacket.class,
                WaterFloatInputPacket::encode,
                WaterFloatInputPacket::decode,
                WaterFloatInputPacket::handle);
        initialized = true;
    }

    public static void sendWaterFloatInput(boolean held) {
        CHANNEL.sendToServer(new WaterFloatInputPacket(held));
    }
}
