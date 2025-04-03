package com.floye.whitelistmod;

import com.floye.whitelistmod.networking.ClientModListPayload;
import com.floye.whitelistmod.networking.ServerHelloPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Arrays;

public class ModCheckerServer implements ModInitializer {

	private static List<String> allowedMods;

	@Override
	public void onInitialize() {
		loadConfig(); // Charger la configuration au démarrage

		// Enregistrement des Payloads pour les messages serveur -> client et client -> serveur
		PayloadTypeRegistry.playC2S().register(ClientModListPayload.ID, ClientModListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHelloPayload.PACKET_ID, ServerHelloPayload.PACKET_CODEC);

		// Réception des mods du client et gestion de la connexion
		ServerPlayNetworking.registerGlobalReceiver(ClientModListPayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			List<String> clientMods = payload.modIds(); // Liste des mods envoyée par le client

			if (!isClientAllowed(clientMods)) {
				String rejectMessage = "Mods non autorisés. Mods requis: " + allowedMods.toString();
				context.server().execute(() -> player.networkHandler.disconnect(Text.literal(rejectMessage))); // Déconnexion du joueur
			} else {
				// Réponse positive du serveur (facultatif)
				ServerHelloPayload responsePayload = new ServerHelloPayload(true, "Connexion autorisée");
				ServerPlayNetworking.send(player, responsePayload); // Envoi de la réponse au client
			}
		});
	}

	// Chargement de la configuration des mods autorisés
	private void loadConfig() {
		File configFile = FabricLoader.getInstance().getConfigDir().resolve("modchecker/allowed_mods.json").toFile();
		if (configFile.exists()) {
			try (FileReader reader = new FileReader(configFile)) {
				Gson gson = new Gson();
				Type listType = new TypeToken<List<String>>(){}.getType(); // Type pour une liste de chaînes avec Gson
				allowedMods = gson.fromJson(reader, listType);
			} catch (IOException e) {
				System.err.println("Erreur lors de la lecture de la config ModChecker: " + e.getMessage());
				allowedMods = Arrays.asList("fabric-api"); // Configuration par défaut en cas d'erreur
			}
		} else {
			allowedMods = Arrays.asList("fabric-api"); // Configuration par défaut si le fichier n'existe pas
		}
	}

	// Vérifie si tous les mods nécessaires sont présents chez le client
	private boolean isClientAllowed(List<String> clientMods) {
		if (allowedMods == null || allowedMods.isEmpty()) return true;

		// Vérifie si TOUS les mods autorisés sont présents chez le client
		return clientMods.containsAll(allowedMods);
	}
}
