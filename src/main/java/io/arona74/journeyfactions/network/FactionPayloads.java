package io.arona74.journeyfactions.network;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Network payloads for faction data synchronization (1.21+ compatible)
 */
public class FactionPayloads {

    public static final String MOD_ID = "factions";

    /**
     * Chunk data for network transfer
     */
    public record ChunkData(int x, int z) {
        public static final PacketCodec<RegistryByteBuf, ChunkData> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ChunkData::x,
                PacketCodecs.VAR_INT, ChunkData::z,
                ChunkData::new
        );
    }

    /**
     * Single faction data for network transfer
     */
    public record FactionData(
            String id,
            String name,
            String displayName,
            int typeOrdinal,
            boolean hasColor,
            int colorRgb,
            List<ChunkData> chunks
    ) {
        // Manual codec for 7+ fields
        public static final PacketCodec<RegistryByteBuf, FactionData> CODEC = new PacketCodec<>() {
            @Override
            public FactionData decode(RegistryByteBuf buf) {
                String id = PacketCodecs.STRING.decode(buf);
                String name = PacketCodecs.STRING.decode(buf);
                String displayName = PacketCodecs.STRING.decode(buf);
                int typeOrdinal = PacketCodecs.VAR_INT.decode(buf);
                boolean hasColor = PacketCodecs.BOOL.decode(buf);
                int colorRgb = PacketCodecs.VAR_INT.decode(buf);

                int chunkCount = PacketCodecs.VAR_INT.decode(buf);
                List<ChunkData> chunks = new ArrayList<>(chunkCount);
                for (int i = 0; i < chunkCount; i++) {
                    chunks.add(ChunkData.CODEC.decode(buf));
                }

                return new FactionData(id, name, displayName, typeOrdinal, hasColor, colorRgb, chunks);
            }

            @Override
            public void encode(RegistryByteBuf buf, FactionData data) {
                PacketCodecs.STRING.encode(buf, data.id);
                PacketCodecs.STRING.encode(buf, data.name);
                PacketCodecs.STRING.encode(buf, data.displayName);
                PacketCodecs.VAR_INT.encode(buf, data.typeOrdinal);
                PacketCodecs.BOOL.encode(buf, data.hasColor);
                PacketCodecs.VAR_INT.encode(buf, data.colorRgb);

                PacketCodecs.VAR_INT.encode(buf, data.chunks.size());
                for (ChunkData chunk : data.chunks) {
                    ChunkData.CODEC.encode(buf, chunk);
                }
            }
        };
    }

    /**
     * Full faction data sync payload (server -> client)
     */
    public record FactionDataSyncPayload(List<FactionData> factions) implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "faction_data_sync");
        public static final CustomPayload.Id<FactionDataSyncPayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, FactionDataSyncPayload> CODEC = new PacketCodec<>() {
            @Override
            public FactionDataSyncPayload decode(RegistryByteBuf buf) {
                int count = PacketCodecs.VAR_INT.decode(buf);
                List<FactionData> factions = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    factions.add(FactionData.CODEC.decode(buf));
                }
                return new FactionDataSyncPayload(factions);
            }

            @Override
            public void encode(RegistryByteBuf buf, FactionDataSyncPayload payload) {
                PacketCodecs.VAR_INT.encode(buf, payload.factions.size());
                for (FactionData faction : payload.factions) {
                    FactionData.CODEC.encode(buf, faction);
                }
            }
        };

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Single faction update payload (server -> client)
     */
    public record FactionUpdatePayload(FactionData faction) implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "faction_update");
        public static final CustomPayload.Id<FactionUpdatePayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, FactionUpdatePayload> CODEC = FactionData.CODEC.xmap(
                FactionUpdatePayload::new,
                FactionUpdatePayload::faction
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Chunk claim payload (server -> client)
     */
    public record ChunkClaimPayload(String factionId, int chunkX, int chunkZ) implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "chunk_claim");
        public static final CustomPayload.Id<ChunkClaimPayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, ChunkClaimPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.STRING, ChunkClaimPayload::factionId,
                PacketCodecs.VAR_INT, ChunkClaimPayload::chunkX,
                PacketCodecs.VAR_INT, ChunkClaimPayload::chunkZ,
                ChunkClaimPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Chunk unclaim payload (server -> client)
     */
    public record ChunkUnclaimPayload(int chunkX, int chunkZ) implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "chunk_unclaim");
        public static final CustomPayload.Id<ChunkUnclaimPayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, ChunkUnclaimPayload> CODEC = PacketCodec.tuple(
                PacketCodecs.VAR_INT, ChunkUnclaimPayload::chunkX,
                PacketCodecs.VAR_INT, ChunkUnclaimPayload::chunkZ,
                ChunkUnclaimPayload::new
        );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Faction delete payload (server -> client)
     */
    public record FactionDeletePayload(String factionId) implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "faction_delete");
        public static final CustomPayload.Id<FactionDeletePayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, FactionDeletePayload> CODEC = new PacketCodec<>() {
            @Override
            public FactionDeletePayload decode(RegistryByteBuf buf) {
                return new FactionDeletePayload(PacketCodecs.STRING.decode(buf));
            }

            @Override
            public void encode(RegistryByteBuf buf, FactionDeletePayload payload) {
                PacketCodecs.STRING.encode(buf, payload.factionId);
            }
        };

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Client request for faction data (client -> server)
     */
    public record ClientRequestDataPayload() implements CustomPayload {
        public static final Identifier PACKET_ID = Identifier.of(MOD_ID, "client_request_data");
        public static final CustomPayload.Id<ClientRequestDataPayload> ID = new CustomPayload.Id<>(PACKET_ID);
        public static final PacketCodec<RegistryByteBuf, ClientRequestDataPayload> CODEC = PacketCodec.unit(new ClientRequestDataPayload());

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
