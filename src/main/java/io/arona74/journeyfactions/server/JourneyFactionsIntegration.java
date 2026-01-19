package io.arona74.journeyfactions.server;

import io.arona74.journeyfactions.JourneyFactionsMain;
import io.arona74.journeyfactions.network.FactionPayloads.*;
import io.icker.factions.api.persistents.Claim;
import io.icker.factions.api.persistents.Faction;
import io.icker.factions.core.FactionsManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles network communication to JourneyFactions client mod
 */
public class JourneyFactionsIntegration {

    public static void initialize() {
        JourneyFactionsMain.LOGGER.info("Initializing JourneyFactions integration...");

        // Note: Payload types are registered in JourneyFactionsMain.registerNetworkPayloads()

        // Handle client requests for faction data
        ServerPlayNetworking.registerGlobalReceiver(ClientRequestDataPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            JourneyFactionsMain.LOGGER.info("Player {} requested factions data for JourneyMap", player.getName().getString());

            context.server().execute(() -> {
                sendFactionDataToPlayer(player);
            });
        });

        // Send faction data when players join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            // Small delay to ensure client is ready
            server.execute(() -> {
                sendFactionDataToPlayer(handler.player);
            });
        });

        // Register event listeners for faction changes
        JourneyFactionsEventListeners.register();

        JourneyFactionsMain.LOGGER.info("JourneyFactions integration initialized successfully");
    }

    /**
     * Send all faction data to a player
     */
    public static void sendFactionDataToPlayer(ServerPlayerEntity player) {
        try {
            JourneyFactionsMain.LOGGER.debug("Sending factions data to player: {}", player.getName().getString());

            // Get all factions
            Collection<Faction> allFactions = Faction.all();
            List<FactionData> factionDataList = new ArrayList<>(allFactions.size());

            JourneyFactionsMain.LOGGER.debug("Sending {} factions to {}", allFactions.size(), player.getName().getString());

            for (Faction faction : allFactions) {
                factionDataList.add(createFactionData(faction));
            }

            ServerPlayNetworking.send(player, new FactionDataSyncPayload(factionDataList));
            JourneyFactionsMain.LOGGER.debug("Factions data sent successfully to {}", player.getName().getString());

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error sending factions data to player: " + player.getName().getString(), e);
        }
    }

    /**
     * Broadcast faction update to all players
     */
    public static void broadcastFactionUpdate(Faction faction) {
        try {
            JourneyFactionsMain.LOGGER.debug("Broadcasting factions update: {}", faction.getName());

            FactionUpdatePayload payload = new FactionUpdatePayload(createFactionData(faction));

            // Send to all online players
            for (ServerPlayerEntity player : FactionsManager.playerManager.getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error broadcasting factions update for: " + faction.getName(), e);
        }
    }

    /**
     * Broadcast chunk claim to all players
     */
    public static void broadcastChunkClaim(ChunkPos chunk, Faction faction) {
        try {
            JourneyFactionsMain.LOGGER.debug("Broadcasting chunk claim: {} by {}", chunk, faction.getName());

            ChunkClaimPayload payload = new ChunkClaimPayload(faction.getID().toString(), chunk.x, chunk.z);

            // Send to all online players
            for (ServerPlayerEntity player : FactionsManager.playerManager.getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error broadcasting chunk claim", e);
        }
    }

    /**
     * Broadcast chunk unclaim to all players
     */
    public static void broadcastChunkUnclaim(ChunkPos chunk) {
        try {
            JourneyFactionsMain.LOGGER.debug("Broadcasting chunk unclaim: {}", chunk);

            ChunkUnclaimPayload payload = new ChunkUnclaimPayload(chunk.x, chunk.z);

            // Send to all online players
            for (ServerPlayerEntity player : FactionsManager.playerManager.getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error broadcasting chunk unclaim", e);
        }
    }

    /**
     * Broadcast faction deletion to all players
     */
    public static void broadcastFactionDeletion(Faction faction) {
        try {
            JourneyFactionsMain.LOGGER.debug("Broadcasting factions deletion: {}", faction.getName());

            FactionDeletePayload payload = new FactionDeletePayload(faction.getID().toString());

            // Send to all online players
            for (ServerPlayerEntity player : FactionsManager.playerManager.getPlayerList()) {
                ServerPlayNetworking.send(player, payload);
            }

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error broadcasting factions deletion", e);
        }
    }

    /**
     * Create FactionData from a Faction
     */
    private static FactionData createFactionData(Faction faction) {
        String id = faction.getID().toString();
        String name = faction.getName();
        String displayName = getFormattedName(faction);
        int typeOrdinal = getFactionTypeOrdinal(faction);

        Color factionColor = getFactionColor(faction);
        boolean hasColor = (factionColor != null);
        int colorRgb = (factionColor != null) ? factionColor.getRGB() : 0;

        List<ChunkData> chunks = new ArrayList<>();
        for (ChunkPos chunk : getChunkPosFromClaims(faction)) {
            chunks.add(new ChunkData(chunk.x, chunk.z));
        }

        return new FactionData(id, name, displayName, typeOrdinal, hasColor, colorRgb, chunks);
    }

    /**
     * Get formatted faction name with color
     */
    private static String getFormattedName(Faction faction) {
        if (faction.getColor() != null) {
            return faction.getColor() + faction.getName();
        }
        return faction.getName();
    }

    /**
     * Convert faction claims to ChunkPos set
     */
    private static Set<ChunkPos> getChunkPosFromClaims(Faction faction) {
        Set<ChunkPos> chunkPosSet = new HashSet<>();

        try {
            // Get the claims list and convert to ChunkPos
            List<Claim> claims = faction.getClaims();
            for (Claim claim : claims) {
                // Use the public x and z fields directly
                chunkPosSet.add(new ChunkPos(claim.x, claim.z));
            }
        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.error("Error converting factions claims to ChunkPos", e);
        }

        return chunkPosSet;
    }

    /**
     * Convert faction to client faction type ordinal
     */
    private static int getFactionTypeOrdinal(Faction faction) {
        String factionName = faction.getName().toLowerCase();

        if (factionName.equals("wilderness")) {
            return 1; // WILDERNESS
        } else if (factionName.equals("safezone")) {
            return 2; // SAFEZONE
        } else if (factionName.equals("warzone")) {
            return 3; // WARZONE
        } else {
            return 0; // PLAYER (default)
        }
    }

    /**
     * Extract color from faction
     */
    private static Color getFactionColor(Faction faction) {
        try {
            // Check if faction has specific colors based on type
            String factionName = faction.getName().toLowerCase();
            if (factionName.equals("wilderness")) {
                return new Color(100, 100, 100); // Gray
            } else if (factionName.equals("safezone")) {
                return new Color(0, 255, 0); // Green
            } else if (factionName.equals("warzone")) {
                return new Color(255, 0, 0); // Red
            }

            // For player factions, get color from faction.getColor()
            if (faction.getColor() != null) {
                return parseMinecraftFormattingColor(faction.getColor());
            }

            // Fallback: generate color from faction name hash
            int hash = faction.getName().hashCode();
            float hue = Math.abs(hash % 360) / 360.0f;
            return Color.getHSBColor(hue, 0.7f, 0.9f);

        } catch (Exception e) {
            JourneyFactionsMain.LOGGER.debug("Could not extract color for faction {}, using fallback", faction.getName());
            // Fallback color
            int hash = faction.getName().hashCode();
            float hue = Math.abs(hash % 360) / 360.0f;
            return Color.getHSBColor(hue, 0.7f, 0.9f);
        }
    }

    /**
     * Parse Minecraft Formatting enum to Java Color
     */
    private static Color parseMinecraftFormattingColor(net.minecraft.util.Formatting formatting) {
        switch (formatting) {
            case BLACK: return new Color(0, 0, 0);
            case DARK_BLUE: return new Color(0, 0, 170);
            case DARK_GREEN: return new Color(0, 170, 0);
            case DARK_AQUA: return new Color(0, 170, 170);
            case DARK_RED: return new Color(170, 0, 0);
            case DARK_PURPLE: return new Color(170, 0, 170);
            case GOLD: return new Color(255, 170, 0);
            case GRAY: return new Color(170, 170, 170);
            case DARK_GRAY: return new Color(85, 85, 85);
            case BLUE: return new Color(85, 85, 255);
            case GREEN: return new Color(85, 255, 85);
            case AQUA: return new Color(85, 255, 255);
            case RED: return new Color(255, 85, 85);
            case LIGHT_PURPLE: return new Color(255, 85, 255);
            case YELLOW: return new Color(255, 255, 85);
            case WHITE: return new Color(255, 255, 255);
            default:
                // Fallback for non-color formatting codes
                return new Color(255, 255, 255);
        }
    }
}
