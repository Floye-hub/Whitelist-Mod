package com.floye.whitelistmod;

import com.floye.whitelistmod.networking.ClientModListPayload;
import com.floye.whitelistmod.networking.ServerHelloPayload;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.fabricmc.loader.api.FabricLoader;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ModCheckerServer implements DedicatedServerModInitializer {

	private static List<String> allowedMods;
	private static List<String> necessaryMods;
	private static String language = "en_us"; // Langue par défaut

	@Override
	public void onInitializeServer() {
		// Utilisation de la traduction pour le message d'initialisation
		System.out.println(getTranslatedMessage("modchecker.server.initialized"));
		loadConfig();

		// Enregistrement des Payloads pour client -> serveur et serveur -> client
		PayloadTypeRegistry.playC2S().register(ClientModListPayload.ID, ClientModListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHelloPayload.PACKET_ID, ServerHelloPayload.PACKET_CODEC);

		// Réception des mods du client
		ServerPlayNetworking.registerGlobalReceiver(ClientModListPayload.ID, (payload, context) -> {
			final ServerPlayerEntity player = context.player();
			List<String> clientMods = payload.modIds();

			// Utilisation des traductions pour les logs
			System.out.println(getTranslatedMessage("modchecker.client.mods_received", clientMods));
			System.out.println(getTranslatedMessage("modchecker.client.mods_detected", player.getName().getString(), clientMods));

			final RejectionResult rejectionResult = isClientAllowed(clientMods);

			if (!rejectionResult.allowed()) {
				// Construction du message de rejet avec les traductions
				StringBuilder rejectMessage = new StringBuilder(getTranslatedMessage("modchecker.connection.response.denied", ""));

				if (!rejectionResult.missingMods().isEmpty()) {
					rejectMessage.append(" ")
							.append(getTranslatedMessage("modchecker.connection.reason.missing_mods", rejectionResult.missingMods()));
				}
				if (!rejectionResult.extraMods().isEmpty()) {
					rejectMessage.append(" ")
							.append(getTranslatedMessage("modchecker.connection.reason.extra_mods", rejectionResult.extraMods()));
				}

				// Déconnexion du joueur avec un message localisé
				context.server().execute(() -> player.networkHandler.disconnect(Text.literal(rejectMessage.toString())));
			} else {
				// Envoyer un message d'acceptation avec une traduction
				ServerHelloPayload responsePayload = new ServerHelloPayload(true, getTranslatedMessage("modchecker.connection.response.success"));
				ServerPlayNetworking.send(player, responsePayload);
				System.out.println(getTranslatedMessage("modchecker.connection.allowed", player.getName().getString()));
			}
		});
	}

	private void loadConfig() {
		// Chemin du fichier de configuration
		File configFile = FabricLoader.getInstance().getConfigDir().resolve("Whitelistmod/Whitelistmod_config.json").toFile();
		File configDir = configFile.getParentFile();

		if (!configFile.exists()) {
			System.out.println("[ModChecker] Configuration file not found. Creating default configuration.");
			if (!configDir.exists()) {
				configDir.mkdirs();
			}

			// Création de la configuration par défaut
			ModCheckerConfig defaultConfig = new ModCheckerConfig();
			defaultConfig.USE_WHITELIST_ONLY = true;
			defaultConfig.CLIENT_MOD_NECESSARY = List.of("whitelistmod");
			defaultConfig.CLIENT_MOD_WHITELIST = List.of("fabric-api");
			defaultConfig.CLIENT_MOD_BLACKLIST = List.of("aristois", "bleachhack");
			defaultConfig.LANGUAGE = "en_us"; // Langue par défaut

			try (FileWriter writer = new FileWriter(configFile)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				gson.toJson(defaultConfig, writer);
				System.out.println("[ModChecker] Default configuration created: Whitelistmod_config.json");
			} catch (IOException e) {
				System.err.println("[ModChecker] Error creating default configuration: " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (configFile.exists()) {
			try (FileReader reader = new FileReader(configFile)) {
				Gson gson = new Gson();
				ModCheckerConfig config = gson.fromJson(reader, ModCheckerConfig.class);

				allowedMods = config.CLIENT_MOD_WHITELIST;
				necessaryMods = config.CLIENT_MOD_NECESSARY;
				language = config.LANGUAGE; // Charger la langue depuis la configuration

				System.out.println("[ModChecker] Configuration loaded. Language set to: " + language);
			} catch (IOException e) {
				System.err.println("[ModChecker] Error reading configuration: " + e.getMessage());
				e.printStackTrace();
				allowedMods = List.of("fabric-api");
				necessaryMods = List.of("whitelistmod");
				language = "en_us"; // Langue par défaut
			}
		}
	}

	private String getTranslatedMessage(String key, Object... args) {
		// Crée une traduction basée sur la langue sélectionnée
		return Text.translatable(key, args).getString();
	}

	private RejectionResult isClientAllowed(List<String> clientMods) {
		Set<String> clientModsSet = new HashSet<>(clientMods);
		Set<String> allowedModsSet = new HashSet<>(allowedMods);
		Set<String> necessaryModsSet = new HashSet<>(necessaryMods);

		// Vérifier les mods manquants
		List<String> missingNecessaryMods = necessaryModsSet.stream()
				.filter(mod -> !clientModsSet.contains(mod))
				.collect(Collectors.toList());

		// Vérifier les mods en trop
		Set<String> allowedClientMods = new HashSet<>(clientModsSet);
		allowedClientMods.removeAll(necessaryModsSet); // Retirer les mods nécessaires
		List<String> extraUnauthorizedMods = allowedClientMods.stream()
				.filter(mod -> !allowedModsSet.contains(mod))
				.collect(Collectors.toList());

		// Retourner un rejet si un problème est détecté
		if (!missingNecessaryMods.isEmpty() || !extraUnauthorizedMods.isEmpty()) {
			return new RejectionResult(false, missingNecessaryMods, extraUnauthorizedMods);
		}

		return new RejectionResult(true, List.of(), List.of()); // Tout est bon
	}

	private record RejectionResult(boolean allowed, List<String> missingMods, List<String> extraMods) {}
}