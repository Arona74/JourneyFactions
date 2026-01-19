package io.arona74.journeyfactions.network;

import io.arona74.journeyfactions.JourneyFactions;
import io.arona74.journeyfactions.data.ClientFaction;
import io.arona74.journeyfactions.network.FactionPayloads.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.ChunkPos;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * Handles network communication from server-side factions mod
 */
public class ClientNetworkHandler {

    public static void initialize() {
        JourneyFactions.debugLog("Initializing client network handlers...");

        // Note: Payload types are registered in the common initializer (JourneyFactionsMain)
        // We only register the client-side receivers here

        // Register packet receivers
        registerPacketHandlers();

        // Register connection events
        registerConnectionEvents();

        JourneyFactions.debugLog("Client network handler initialized successfully");
    }

    private static void registerPacketHandlers() {
        // Handle full faction data sync (sent on join or request)
        ClientPlayNetworking.registerGlobalReceiver(FactionDataSyncPayload.ID, (payload, context) -> {
            try {
                JourneyFactions.debugLog("Receiving full faction data sync: {} factions", payload.factions().size());

                // Read all factions
                Set<ClientFaction> factions = new HashSet<>();
                for (FactionData data : payload.factions()) {
                    ClientFaction faction = convertToClientFaction(data);
                    if (faction != null) {
                        factions.add(faction);
                        JourneyFactions.debugLog("Received faction: {} with {} chunks", faction.getName(), faction.getClaimedChunks().size());
                    }
                }

                // Process on main thread
                context.client().execute(() -> {
                    try {
                        // Clear existing data
                        JourneyFactions.getFactionManager().clear();

                        // Add all received factions
                        for (ClientFaction faction : factions) {
                            JourneyFactions.getFactionManager().addOrUpdateFaction(faction);
                        }

                        JourneyFactions.debugLog("Successfully processed {} factions from server", factions.size());

                    } catch (Exception e) {
                        JourneyFactions.LOGGER.error("Error processing faction data sync", e);
                    }
                });

            } catch (Exception e) {
                JourneyFactions.LOGGER.error("Error reading faction data sync packet", e);
            }
        });

        // Handle individual faction updates
        ClientPlayNetworking.registerGlobalReceiver(FactionUpdatePayload.ID, (payload, context) -> {
            try {
                ClientFaction faction = convertToClientFaction(payload.faction());
                if (faction != null) {
                    JourneyFactions.debugLog("Received faction update: {}", faction.getName());

                    context.client().execute(() -> {
                        JourneyFactions.getFactionManager().addOrUpdateFaction(faction);
                    });
                }
            } catch (Exception e) {
                JourneyFactions.LOGGER.error("Error processing faction update", e);
            }
        });

        // Handle chunk claims
        ClientPlayNetworking.registerGlobalReceiver(ChunkClaimPayload.ID, (payload, context) -> {
            try {
                String factionId = payload.factionId();
                ChunkPos chunk = new ChunkPos(payload.chunkX(), payload.chunkZ());
                JourneyFactions.debugLog("Received chunk claim: {} by faction {}", chunk, factionId);

                context.client().execute(() -> {
                    JourneyFactions.getFactionManager().setChunkOwner(chunk, factionId);
                });

            } catch (Exception e) {
                JourneyFactions.LOGGER.error("Error processing chunk claim", e);
            }
        });

        // Handle chunk unclaims
        ClientPlayNetworking.registerGlobalReceiver(ChunkUnclaimPayload.ID, (payload, context) -> {
            try {
                ChunkPos chunk = new ChunkPos(payload.chunkX(), payload.chunkZ());
                JourneyFactions.debugLog("Received chunk unclaim: {}", chunk);

                context.client().execute(() -> {
                    // Set to wilderness (null means wilderness)
                    JourneyFactions.getFactionManager().setChunkOwner(chunk, null);
                });

            } catch (Exception e) {
                JourneyFactions.LOGGER.error("Error processing chunk unclaim", e);
            }
        });

        // Handle faction deletions
        ClientPlayNetworking.registerGlobalReceiver(FactionDeletePayload.ID, (payload, context) -> {
            try {
                String factionId = payload.factionId();
                JourneyFactions.debugLog("Received faction deletion: {}", factionId);

                context.client().execute(() -> {
                    JourneyFactions.getFactionManager().removeFaction(factionId);
                });

            } catch (Exception e) {
                JourneyFactions.LOGGER.error("Error processing faction deletion", e);
            }
        });

        JourneyFactions.debugLog("Registered all packet handlers");
    }

    private static void registerConnectionEvents() {
        // Request faction data when joining a server
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            JourneyFactions.debugLog("Connected to server - requesting faction data");

            // Small delay to ensure everything is initialized
            new Thread(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                    requestFactionData();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    JourneyFactions.debugLog("Interrupted while waiting to request faction data");
                }
            }).start();
        });

        // Clear data when disconnecting
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            JourneyFactions.debugLog("Disconnected from server - clearing faction data");
            JourneyFactions.getFactionManager().clear();
        });

        JourneyFactions.debugLog("Registered connection event handlers");
    }

    /**
     * Request faction data from server
     */
    public static void requestFactionData() {
        try {
            JourneyFactions.debugLog("Requesting faction data from server");

            ClientPlayNetworking.send(new ClientRequestDataPayload());
            JourneyFactions.debugLog("Faction data request sent");

        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Failed to request faction data", e);
        }
    }

    /**
     * Convert FactionData to ClientFaction
     */
    private static ClientFaction convertToClientFaction(FactionData data) {
        try {
            ClientFaction faction = new ClientFaction(data.id(), data.name());
            faction.setDisplayName(data.displayName());

            // Set faction type
            ClientFaction.FactionType type = getFactionTypeFromOrdinal(data.typeOrdinal());
            faction.setType(type);

            // Set color if present
            if (data.hasColor()) {
                faction.setColor(new Color(data.colorRgb()));
            }

            // Set claimed chunks
            Set<ChunkPos> chunks = new HashSet<>();
            for (ChunkData chunk : data.chunks()) {
                chunks.add(new ChunkPos(chunk.x(), chunk.z()));
            }
            faction.setClaimedChunks(chunks);

            JourneyFactions.debugLog("Converted faction from data: {} ({}) with {} chunks", data.name(), type, chunks.size());

            return faction;

        } catch (Exception e) {
            JourneyFactions.LOGGER.error("Error converting faction data", e);
            return null;
        }
    }

    /**
     * Convert ordinal back to faction type (matches server-side getFactionTypeOrdinal)
     */
    private static ClientFaction.FactionType getFactionTypeFromOrdinal(int ordinal) {
        switch (ordinal) {
            case 1: return ClientFaction.FactionType.WILDERNESS;
            case 2: return ClientFaction.FactionType.SAFEZONE;
            case 3: return ClientFaction.FactionType.WARZONE;
            default: return ClientFaction.FactionType.PLAYER;
        }
    }
}
