package us.myles.ViaVersion.protocols.protocol1_14to1_13_2;

import us.myles.ViaVersion.api.Via;
import us.myles.ViaVersion.api.data.UserConnection;
import us.myles.ViaVersion.api.entities.Entity1_14Types;
import us.myles.ViaVersion.api.minecraft.VillagerData;
import us.myles.ViaVersion.api.minecraft.item.Item;
import us.myles.ViaVersion.api.minecraft.metadata.Metadata;
import us.myles.ViaVersion.api.minecraft.metadata.types.MetaType1_14;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.packets.InventoryPackets;
import us.myles.ViaVersion.protocols.protocol1_14to1_13_2.storage.EntityTracker;

import java.util.ArrayList;
import java.util.List;

public class MetadataRewriter {

    public static void handleMetadata(int entityId, Entity1_14Types.EntityType type, List<Metadata> metadatas, UserConnection connection) {
        for (Metadata metadata : new ArrayList<>(metadatas)) {
            try {
                metadata.setMetaType(MetaType1_14.byId(metadata.getMetaType().getTypeID()));

                EntityTracker tracker = connection.get(EntityTracker.class);
                // 1.13 changed item to flat item (no data)
                if (metadata.getMetaType() == MetaType1_14.Slot) {
                    InventoryPackets.toClient((Item) metadata.getValue());
                } else if (metadata.getMetaType() == MetaType1_14.BlockID) {
                    // Convert to new block id
                    int data = (int) metadata.getValue();
                    metadata.setValue(Protocol1_14To1_13_2.getNewBlockStateId(data));
                }

                if (type == null) continue;

                //Metadata 6 added to abstract_entity
                if (metadata.getId() > 5) {
                    metadata.setId(metadata.getId() + 1);
                }

                //Metadata 12 added to living_entity
                if (metadata.getId() > 11 && type.isOrHasParent(Entity1_14Types.EntityType.LIVINGENTITY)) {
                    metadata.setId(metadata.getId() + 1);
                }

                if (type.isOrHasParent(Entity1_14Types.EntityType.MINECART_ABSTRACT)) {
                    if (metadata.getId() == 10) {
                        // New block format
                        int data = (int) metadata.getValue();
                        metadata.setValue(Protocol1_14To1_13_2.getNewBlockStateId(data));
                    }
                }

                if (type.is(Entity1_14Types.EntityType.HORSE)) {
                    if (metadata.getId() == 18) {
                        metadatas.remove(metadata);  //TODO Probably sent as entity equipment now
                    }
                }

                if (type.is(Entity1_14Types.EntityType.VILLAGER)) {
                    if (metadata.getId() == 15) {
                        // plains
                        metadata.setValue(new VillagerData(2, getNewProfessionId((int) metadata.getValue()), 0));
                        metadata.setMetaType(MetaType1_14.VillagerData);
                    }
                } else if (type.is(Entity1_14Types.EntityType.ZOMBIE_VILLAGER)) {
                    if (metadata.getId() == 19) {
                        // plains
                        metadata.setValue(new VillagerData(2, getNewProfessionId((int) metadata.getValue()), 0));
                        metadata.setMetaType(MetaType1_14.VillagerData);
                    }
                }

                if (type.isOrHasParent(Entity1_14Types.EntityType.ARROW)) {
                    if (metadata.getId() >= 9) {
                        metadata.setId(metadata.getId() + 1);
                    }
                }

                if (type.is(Entity1_14Types.EntityType.FIREWORKS_ROCKET)) {
                    if (metadata.getId() == 8) {
                        if (metadata.getValue().equals(0)) metadata.setValue(null); // https://bugs.mojang.com/browse/MC-111480
                        metadata.setMetaType(MetaType1_14.OptVarInt);
                    }
                }
            } catch (Exception e) {
                metadatas.remove(metadata);
                if (!Via.getConfig().isSuppressMetadataErrors() || Via.getManager().isDebug()) {
                    Via.getPlatform().getLogger().warning("An error occurred with entity metadata handler");
                    Via.getPlatform().getLogger().warning("Metadata: " + metadata);
                    e.printStackTrace();
                }
            }
        }
    }

    private static int getNewProfessionId(int old) {
        // profession -> career
        switch (old) {
            case 0: // farmer
                return 5;
            case 1: // librarian
                return 9;
            case 2: // priest
                return 4; // cleric
            case 3: // blacksmith
                return 1; // armorer
            case 4: // butcher
                return 2;
            case 5: // nitwit
                return 11;
            default:
                return 0; // none
        }
    }

}
