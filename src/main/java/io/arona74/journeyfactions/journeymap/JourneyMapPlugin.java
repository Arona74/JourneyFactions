package io.arona74.journeyfactions.journeymap;

import io.arona74.journeyfactions.JourneyFactions;
import journeymap.api.v2.client.IClientAPI;
import journeymap.api.v2.client.IClientPlugin;
import journeymap.api.v2.client.event.MappingEvent;
import journeymap.api.v2.client.event.DisplayUpdateEvent;
import journeymap.api.v2.common.event.ClientEventRegistry;

@journeymap.api.v2.client.JourneyMapPlugin(apiVersion = "2.0.0")
public class JourneyMapPlugin implements IClientPlugin {

    private static final String PLUGIN_ID = "journeyfactions";
    private IClientAPI jmAPI;
    private FactionOverlayManager overlayManager;

    public JourneyMapPlugin() {
        JourneyFactions.debugLog("JourneyMapPlugin constructor called");
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        JourneyFactions.debugLog("JourneyMapPlugin.initialize() called");

        this.jmAPI = jmClientApi;
        this.overlayManager = new FactionOverlayManager(jmClientApi);

        JourneyFactions.debugLog("JourneyMap integration initialized");

        try {
            // Subscribe to mapping events using the new API 2.0 event system
            ClientEventRegistry.MAPPING_EVENT.subscribe(getModId(), this::onMappingEvent);
            ClientEventRegistry.DISPLAY_UPDATE_EVENT.subscribe(getModId(), this::onDisplayUpdateEvent);
            JourneyFactions.debugLog("Subscribed to JourneyMap events");

            // Connect to faction manager for updates
            JourneyFactions.getFactionManager().addListener(overlayManager);
            JourneyFactions.debugLog("Connected to faction manager");

            // Initialize the faction toggle button/keybinding
            FactionToggleButton.initialize(jmClientApi);

            // Initialize context menu integration
            JourneyMapContextIntegration.initialize(jmClientApi);

            JourneyFactions.debugLog("Plugin initialization complete - waiting for mapping events");

        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error during JourneyMap plugin initialization", e);
        }
    }

    @Override
    public String getModId() {
        return PLUGIN_ID;
    }

    /**
     * Handle mapping events (started/stopped)
     */
    private void onMappingEvent(MappingEvent event) {
        JourneyFactions.debugLog("JourneyMap mapping event received: {}", event.getStage());

        try {
            switch (event.getStage()) {
                case MAPPING_STARTED:
                    JourneyFactions.debugLog("JourneyMap mapping started - creating overlays");
                    overlayManager.onMappingStarted();
                    break;
                case MAPPING_STOPPED:
                    JourneyFactions.debugLog("JourneyMap mapping stopped");
                    overlayManager.onMappingStopped();
                    break;
            }
        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error handling JourneyMap mapping event: " + event.getStage(), e);
        }
    }

    /**
     * Handle display update events
     */
    private void onDisplayUpdateEvent(DisplayUpdateEvent event) {
        JourneyFactions.debugLog("JourneyMap display update event received");

        try {
            overlayManager.updateDisplay();
        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error handling JourneyMap display update event", e);
        }
    }

    /**
     * Get the JourneyMap API instance
     */
    public IClientAPI getAPI() {
        return jmAPI;
    }

    /**
     * Get the faction overlay manager
     */
    public FactionOverlayManager getOverlayManager() {
        return overlayManager;
    }

    /**
     * Check if the plugin is properly initialized
     */
    public boolean isInitialized() {
        return jmAPI != null && overlayManager != null;
    }

    /**
     * Get plugin information
     */
    public String getPluginInfo() {
        return String.format("JourneyFactions Plugin | API: %s | Overlays: %d",
            jmAPI != null ? "Connected" : "Disconnected",
            overlayManager != null ? overlayManager.getOverlayCount() : 0);
    }

    /**
     * Cleanup method for plugin shutdown
     */
    public void cleanup() {
        try {
            if (overlayManager != null) {
                overlayManager.clearAllOverlays();
                JourneyFactions.getFactionManager().removeListener(overlayManager);
            }

            FactionToggleButton.cleanup();
            JourneyMapContextIntegration.cleanup();

            JourneyFactions.LOGGER.info("JourneyMapPlugin cleanup completed");

        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error during plugin cleanup", e);
        }
    }
}
