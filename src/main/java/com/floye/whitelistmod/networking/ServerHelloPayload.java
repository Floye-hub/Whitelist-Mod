package com.floye.whitelistmod.networking;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ServerHelloPayload(boolean allowed, String message) implements CustomPayload {


    public static final CustomPayload.Id<ServerHelloPayload> PACKET_ID = new CustomPayload.Id<>(Identifier.of("modchecker", "server_hello"));


    public static final PacketCodec<PacketByteBuf, ServerHelloPayload> PACKET_CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOL,                 // Codec for allowed
                    ServerHelloPayload::allowed,       // Getter for allowed
                    PacketCodecs.STRING,               // Codec for message
                    ServerHelloPayload::message,       // Getter for message
                    ServerHelloPayload::new
            );

    public ServerHelloPayload(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    // Override getId to return the packet's unique ID
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}