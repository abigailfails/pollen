package gg.moonflower.pollen.api.sync.forge;

import gg.moonflower.pollen.api.sync.DataComponent;
import gg.moonflower.pollen.api.sync.SyncedDataManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ForgeDataComponent extends DataComponent {

    public void writeSyncPacket(FriendlyByteBuf buf, ServerPlayer provider, ServerPlayer player) {
        int[] ids = this.values.keySet().stream().filter(key -> (provider == player && key.getSyncStrategy().isSyncEntity()) || key.getSyncStrategy().isSyncTracking()).mapToInt(SyncedDataManager::getId).toArray();
        this.writePacketData(buf, ids);
    }

    public void writeUpdatePacket(FriendlyByteBuf buf, ServerPlayer provider, ServerPlayer player) {
        int[] ids = this.dirtyValues.stream().map(SyncedDataManager::byId).filter(key -> (provider == player && key.getSyncStrategy().isSyncEntity()) || key.getSyncStrategy().isSyncTracking()).mapToInt(SyncedDataManager::getId).toArray();
        this.writePacketData(buf, ids);
    }
}
