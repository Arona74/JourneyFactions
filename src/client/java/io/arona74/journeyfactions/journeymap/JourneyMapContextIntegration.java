package io.arona74.journeyfactions.journeymap;

import io.arona74.journeyfactions.JourneyFactions;
import io.arona74.journeyfactions.client.gui.FactionControlsScreen;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;

import journeymap.client.api.event.fabric.FabricEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Integrates faction controls with JourneyMap's fullscreen map UI
 * using FabricEvents for button display and popup menu events.
 */
public class JourneyMapContextIntegration {

    /**
     * Initialize the context integration by subscribing to JourneyMap Fabric events.
     */
    public static void initialize() {
        FabricEvents.ADDON_BUTTON_DISPLAY_EVENT.register(event -> {
            addButtons(event.getThemeButtonDisplay());
        });

        FabricEvents.FULLSCREEN_POPUP_MENU_EVENT.register(event -> {
            addPopupMenu(event.getPopupMenu());
        });

        JourneyFactions.debugLog("JourneyMap context integration initialized");
    }

    /**
     * Add faction buttons to JourneyMap's fullscreen map toolbar.
     */
    private static void addButtons(ThemeButtonDisplay buttonDisplay) {
        try {
            buttonDisplay.addThemeToggleButton(
                    "Show/Hide Factions",
                    "faction_toggle",
                    FactionDisplayManager.isFactionDisplayEnabled(),
                    b -> {
                        FactionDisplayManager.toggleFactionDisplay();
                        showToggleFeedback();
                    }
            );

            buttonDisplay.addThemeButton(
                    "Faction Settings",
                    "faction_settings",
                    b -> openFactionControlsScreen()
            );

            JourneyFactions.debugLog("Added faction buttons to JourneyMap toolbar");
        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error adding faction buttons: {}", e.getMessage());
        }
    }

    /**
     * Add faction items to JourneyMap's right-click context menu.
     */
    private static void addPopupMenu(ModPopupMenu popupMenu) {
        try {
            ModPopupMenu subMenu = popupMenu.createSubItemList("Faction Controls");

            subMenu.addMenuItem(
                    FactionDisplayManager.isFactionDisplayEnabled() ? "Hide Factions" : "Show Factions",
                    blockPos -> {
                        FactionDisplayManager.toggleFactionDisplay();
                        showToggleFeedback();
                    }
            );

            subMenu.addMenuItem(
                    "Faction Settings",
                    blockPos -> openFactionControlsScreen()
            );

            JourneyFactions.debugLog("Added faction items to JourneyMap context menu");
        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error adding faction menu items: {}", e.getMessage());
        }
    }

    /**
     * Open the faction controls screen.
     */
    private static void openFactionControlsScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null) {
            client.execute(() -> client.setScreen(new FactionControlsScreen()));
        }
    }

    /**
     * Show feedback for toggle action on the action bar.
     */
    private static void showToggleFeedback() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            String message = FactionDisplayManager.isFactionDisplayEnabled()
                    ? "§eFaction territories: §aShown"
                    : "§eFaction territories: §cHidden";
            client.player.sendMessage(
                    net.minecraft.text.Text.literal(message),
                    true
            );
        }
    }

    /**
     * Cleanup method.
     */
    public static void cleanup() {
        JourneyFactions.debugLog("JourneyMap context integration cleanup completed");
    }
}
