package io.arona74.journeyfactions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JourneyFactionsMain implements ModInitializer {
    public static final String MOD_ID = "journeyfactions";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("JourneyFactions initializing...");

        // Check if Factions mod is loaded
        if (FabricLoader.getInstance().isModLoaded("factions")) {
            LOGGER.info("Factions mod detected - initializing server-side integration");

            try {
                // Initialize the factions integration for sending data to clients
                io.arona74.journeyfactions.server.JourneyFactionsIntegration.initialize();
                LOGGER.info("JourneyFactions server integration initialized successfully");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize JourneyFactions server integration", e);
            }
        } else {
            LOGGER.info("Factions mod not detected - server-side integration disabled");
            LOGGER.info("Install the Factions mod on the server to enable faction territory syncing");
        }

        LOGGER.info("JourneyFactions initialization complete");
    }
}
