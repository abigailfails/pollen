package gg.moonflower.pollen.api.sync.forge;

import gg.moonflower.pollen.api.sync.DataComponent;
import gg.moonflower.pollen.api.sync.SyncedDataKey;
import gg.moonflower.pollen.api.sync.SyncedDataManager;
import gg.moonflower.pollen.core.Pollen;
import gg.moonflower.pollen.core.network.PollenMessages;
import gg.moonflower.pollen.core.network.forge.ClientboundUpdateSyncedDataPacket;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@Mod.EventBusSubscriber(modid = Pollen.MOD_ID)
public class SyncedDataManagerImpl {

    @CapabilityInject(ForgeDataComponent.class)
    public static final Capability<ForgeDataComponent> CAPABILITY = null;

    @SuppressWarnings("ConstantConditions")
    private static ForgeDataComponent getDataComponent(Player player) {
        return player.getCapability(CAPABILITY).orElseThrow(() -> new IllegalStateException("Failed to get capability"));
    }

    public static void init() {
        CapabilityManager.INSTANCE.register(ForgeDataComponent.class, new Capability.IStorage<ForgeDataComponent>() {
            @Override
            public Tag writeNBT(Capability<ForgeDataComponent> capability, ForgeDataComponent component, Direction direction) {
                CompoundTag tag = new CompoundTag();
                component.writeToNbt(tag, DataComponent.NbtWriteMode.SAVE);
                return tag;
            }

            @Override
            public void readNBT(Capability<ForgeDataComponent> capability, ForgeDataComponent component, Direction direction, Tag tag) {
                component.readFromNbt((CompoundTag) tag);
            }
        }, ForgeDataComponent::new);
    }

    @SubscribeEvent
    public static void onEvent(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation(Pollen.MOD_ID, "synced_data"), new Provider());
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getPlayer() instanceof ServerPlayer && event.getTarget() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            ServerPlayer target = (ServerPlayer) event.getTarget();
            ForgeDataComponent component = getDataComponent(player);
            if (component.shouldSyncWith(target, player))
                PollenMessages.PLAY.sendTo(player, new ClientboundUpdateSyncedDataPacket(target, player, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerJoinWorld(EntityJoinWorldEvent event) {
        if (event.getEntity() instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ForgeDataComponent component = getDataComponent(player);
            if (component.shouldSyncWith(player, player))
                PollenMessages.PLAY.sendTo(player, new ClientboundUpdateSyncedDataPacket(player, player, true));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer && event.getPlayer() instanceof ServerPlayer) {
            ServerPlayer original = (ServerPlayer) event.getOriginal();
            ServerPlayer player = (ServerPlayer) event.getPlayer();

            ForgeDataComponent oldHolder = getDataComponent(original);
            ForgeDataComponent newHolder = getDataComponent(player);
            if (oldHolder.shouldCopyForRespawn(!event.isWasDeath(), player.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)))
                newHolder.copyForRespawn(oldHolder, !event.isWasDeath());

            if (newHolder.shouldSyncWith(player, player))
                PollenMessages.PLAY.sendTo(player, new ClientboundUpdateSyncedDataPacket(player, player, true));
        }
    }

    public static void sync(ServerPlayer player) {
        ForgeDataComponent component = getDataComponent(player);
        if (component.isDirty()) {
            for (ServerPlayer other : player.getLevel().getServer().getPlayerList().getPlayers()) {
                if (component.shouldSyncWith(player, other))
                    PollenMessages.PLAY.sendTo(other, new ClientboundUpdateSyncedDataPacket(player, other, false));
            }
            component.clean();
        }
    }

    public static <T> void set(Player player, SyncedDataKey<T> key, T value) {
        getDataComponent(player).setValue(key, value);
        SyncedDataManager.markDirty();
    }

    public static <T> T get(Player player, SyncedDataKey<T> key) {
        return getDataComponent(player).getValue(key);
    }

    public static void writePacketData(FriendlyByteBuf buf, ServerPlayer provider, ServerPlayer player, boolean sync) {
        if (sync) {
            getDataComponent(player).writeSyncPacket(buf, provider, player);
        } else {
            getDataComponent(player).writeUpdatePacket(buf, provider, player);
        }
    }

    public static void readPacketData(FriendlyByteBuf buf, Player player) {
        getDataComponent(player).applySyncPacket(buf);
    }

    public static class Provider implements ICapabilitySerializable<CompoundTag> {

        private final ForgeDataComponent component = new ForgeDataComponent();
        private final LazyOptional<ForgeDataComponent> optional = LazyOptional.of(() -> this.component);

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            this.component.writeToNbt(tag, DataComponent.NbtWriteMode.SAVE);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            this.component.readFromNbt(tag);
        }

        @SuppressWarnings("ConstantConditions")
        @NotNull
        @Override
        public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
            return CAPABILITY.orEmpty(capability, this.optional);
        }
    }
}
