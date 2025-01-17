package gg.moonflower.pollen.api.network.packet;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

/**
 * Context for a {@link PollinatedPacket} that acts as an intermediary between platforms.
 *
 * @author Ocelot
 * @since 1.0.0
 */
public interface PollinatedPacketContext {

    /**
     * Defers work if not on the main thread.
     *
     * @param runnable The task to execute
     * @return A future that will complete when the task is done
     */
    CompletableFuture<Void> enqueueWork(Runnable runnable);

    /**
     * Allows blocking client log-in until the future is complete. This has no effect in play.
     *
     * @param future The future that must be done before the player can log in
     * @see net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking.LoginSynchronizer
     */
    void waitFor(Future<?> future);

    /**
     * Sends a packet in response to this packet. This is required for all login packets.
     *
     * @param packet The packet to reply with
     */
    void reply(PollinatedPacket<?> packet);

    /**
     * @return The direction the packet is going
     */
    PollinatedPacketDirection getDirection();

    /**
     * Fetches the server sided player only if the packet has been received server side.
     *
     * @return The player or <code>null</code> if the current handler is not {@link PollinatedPacketDirection#PLAY_SERVERBOUND}
     */
    @Nullable
    default ServerPlayer getSender() {
        PacketListener netHandler = this.getNetworkManager().getPacketListener();
        if (netHandler instanceof ServerGamePacketListenerImpl) {
            ServerGamePacketListenerImpl netHandlerPlayServer = (ServerGamePacketListenerImpl) netHandler;
            return netHandlerPlayServer.player;
        }
        return null;
    }

    /**
     * @return The current connection between the client and server
     */
    Connection getNetworkManager();
}
