package com.floye.whitelistmod.networking;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.Identifier;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record ClientModListPayload(List<String> modIds) implements CustomPayload {
    // L'ID unique de ce payload
    public static final CustomPayload.Id<ClientModListPayload> ID = new CustomPayload.Id<>(Identifier.of("modchecker", "client_mod_list"));

    // Le codec pour sérialiser/désérialiser le payload
    public static final PacketCodec<RegistryByteBuf, ClientModListPayload> CODEC = PacketCodec.tuple(
            PacketCodecs.STRING.collect(PacketCodecs.toList()), // Sérialiser une List<String>
            ClientModListPayload::modIds,
            ClientModListPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID; // Identifie le type de ce payload
    }
}