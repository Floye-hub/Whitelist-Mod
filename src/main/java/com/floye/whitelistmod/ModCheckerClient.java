package com.floye.whitelistmod;

import com.floye.whitelistmod.networking.ClientModListPayload;
import com.floye.whitelistmod.networking.ServerHelloPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.util.List;
import java.util.stream.Collectors;

public class ModCheckerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[ModChecker] ModCheckerClient initialisé."); // Log d'initialisation

        // Enregistrer les Payloads pour client -> serveur et serveur -> client
        PayloadTypeRegistry.playC2S().register(ClientModListPayload.ID, ClientModListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ServerHelloPayload.PACKET_ID, ServerHelloPayload.PACKET_CODEC);

        // Gérer la réception du message de bienvenue ou de refus
        ClientPlayNetworking.registerGlobalReceiver(ServerHelloPayload.PACKET_ID, (payload, context) -> {
            System.out.println("[ModChecker] ServerHelloPayload reçu du serveur.");
            boolean allowed = payload.allowed(); // Si le serveur autorise la connexion
            String message = payload.message(); // Message à afficher

            // Déconnexion et affichage du message si l'accès est refusé
            if (!allowed) {
                clientExecute(() -> {
                    MinecraftClient.getInstance().disconnect(null);
                    MinecraftClient.getInstance().setScreen(new DisconnectedScreen(null, Text.literal("Connexion refusée"), Text.literal(message)));
                });
            } else {
                // Log de confirmation
                System.out.println("[ModChecker] Connexion au serveur autorisée: " + message);
            }
        });

        // Envoi de la liste des mods du client lors de la connexion au serveur
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            System.out.println("[ModChecker] ClientPlayConnectionEvents.JOIN déclenché.");
            List<String> clientModIds = FabricLoader.getInstance().getAllMods().stream()
                    .map(ModContainer::getMetadata)
                    .map(metadata -> metadata.getId())
                    .collect(Collectors.toList());

            // Modification pour ajouter les guillemets doubles
            String modIdsWithQuotes = clientModIds.stream()
                    .map(modId -> "\"" + modId + "\"")
                    .collect(Collectors.joining(", "));

            System.out.println("[ModChecker] Liste des mods du client: " + "[" + modIdsWithQuotes + "]");
            ClientModListPayload modListPayload = new ClientModListPayload(clientModIds);
            System.out.println("[ModChecker] Payload ClientModListPayload créé.");
            ClientPlayNetworking.send(modListPayload); // Envoie des mods au serveur
            System.out.println("[ModChecker] Payload ClientModListPayload envoyé au serveur.");
        });
    }

    private void clientExecute(Runnable runnable) {
        MinecraftClient.getInstance().execute(runnable); // Exécution sur le thread du client
    }
}