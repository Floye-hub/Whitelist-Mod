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

	@Override
	public void onInitializeServer() {
		System.out.println("[ModChecker] ModCheckerServer initialisé."); // Log d'initialisation
		loadConfig(); // Charger la configuration au démarrage

		// Enregistrement des Payloads pour les messages serveur -> client et client -> serveur
		PayloadTypeRegistry.playC2S().register(ClientModListPayload.ID, ClientModListPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(ServerHelloPayload.PACKET_ID, ServerHelloPayload.PACKET_CODEC);

		// Réception des mods du client et gestion de la connexion
		ServerPlayNetworking.registerGlobalReceiver(ClientModListPayload.ID, (payload, context) -> {
			final ServerPlayerEntity player = context.player();
			List<String> clientMods = payload.modIds();

			System.out.println("[ModChecker] Liste des mods reçus du client : " + clientMods);
			System.out.println("[ModChecker] Mods détectés chez le client " + player.getName().getString() + ": " + clientMods);

			final RejectionResult rejectionResult = isClientAllowed(clientMods);
			if (!rejectionResult.allowed()) {
				final StringBuilder rejectMessage = new StringBuilder("[WhitelistMod]");

				if (!rejectionResult.missingMods().isEmpty()) {
					rejectMessage.append("Mods requis manquants: ").append(rejectionResult.missingMods()).append(". ");
				}
				if (!rejectionResult.extraMods().isEmpty()) {
					rejectMessage.append("Mods non autorisés: ").append(rejectionResult.extraMods()).append(". Merci d'ouvrir un ticket pour les faire valider. ");
				}

				context.server().execute(() -> player.networkHandler.disconnect(Text.literal(rejectMessage.toString())));

		} else {
				ServerHelloPayload responsePayload = new ServerHelloPayload(true, "Connexion autorisée");
				ServerPlayNetworking.send(player, responsePayload);
			}
		});

	}

	// Chargement de la configuration des mods autorisés
	private void loadConfig() {
		File configFile = FabricLoader.getInstance().getConfigDir().resolve("modchecker/allowed_mods.json").toFile();
		File configDir = configFile.getParentFile();

		if (!configFile.exists()) {
			System.out.println("[ModChecker] Fichier de configuration non trouvé. Création du fichier par défaut.");
			// Créer le dossier de configuration s'il n'existe pas
			if (!configDir.exists()) {
				configDir.mkdirs();
			}
			// Créer la configuration par défaut
			ModCheckerConfig defaultConfig = new ModCheckerConfig();
			defaultConfig.USE_WHITELIST_ONLY = true;
			defaultConfig.CLIENT_MOD_NECESSARY = Arrays.asList("whitelistmod");
			defaultConfig.CLIENT_MOD_WHITELIST = Arrays.asList("fabric-api");
			defaultConfig.CLIENT_MOD_BLACKLIST = Arrays.asList("aristois", "bleachhack");

			// Sauvegarder la configuration par défaut dans le fichier
			try (FileWriter writer = new FileWriter(configFile)) {
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				gson.toJson(defaultConfig, writer);
				System.out.println("[ModChecker] Fichier de configuration par défaut créé avec succès.");
			} catch (IOException e) {
				System.err.println("Erreur lors de la création du fichier de configuration par défaut: " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (configFile.exists()) {
			try (FileReader reader = new FileReader(configFile)) {
				Gson gson = new Gson();
				System.out.println("[ModChecker] Chargement de la configuration depuis " + configFile.getAbsolutePath());
				ModCheckerConfig config = gson.fromJson(reader, ModCheckerConfig.class);

				allowedMods = config.CLIENT_MOD_WHITELIST;
				necessaryMods = config.CLIENT_MOD_NECESSARY;

				System.out.println("[ModChecker] allowedMods chargés: " + allowedMods);
				System.out.println("[ModChecker] necessaryMods chargés: " + necessaryMods);
				System.out.println("[ModChecker] USE_WHITELIST_ONLY: " + config.USE_WHITELIST_ONLY);
				System.out.println("[ModChecker] CLIENT_MOD_BLACKLIST: " + config.CLIENT_MOD_BLACKLIST);


			} catch (IOException e) {
				System.err.println("Erreur lors de la lecture de la config ModChecker: " + e.getMessage());
				e.printStackTrace();
				allowedMods = Arrays.asList("fabric-api");
				necessaryMods = Arrays.asList("mod_whitelist");
				System.out.println("[ModChecker] Configuration par défaut utilisée.");
			}
		} else {
			System.out.println("[ModChecker] Fichier de configuration non trouvé. Utilisation de la configuration par défaut.");
			allowedMods = Arrays.asList("fabric-api");
			necessaryMods = Arrays.asList("mod_whitelist");
		}
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
		allowedClientMods.removeAll(necessaryModsSet); // Retirer les mods nécessaires pour ne pas les compter comme en trop
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