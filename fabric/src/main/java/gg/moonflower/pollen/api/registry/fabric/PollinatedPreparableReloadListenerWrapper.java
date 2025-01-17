package gg.moonflower.pollen.api.registry.fabric;

import gg.moonflower.pollen.api.registry.PollinatedPreparableReloadListener;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ApiStatus.Internal
public class PollinatedPreparableReloadListenerWrapper implements IdentifiableResourceReloadListener {

    private final PollinatedPreparableReloadListener listener;

    public PollinatedPreparableReloadListenerWrapper(PollinatedPreparableReloadListener listener) {
        this.listener = listener;
    }

    @Override
    public ResourceLocation getFabricId() {
        return this.listener.getPollenId();
    }

    @Override
    public Collection<ResourceLocation> getFabricDependencies() {
        return this.listener.getPollenDependencies();
    }

    @Override
    public String getName() {
        return this.listener.getName();
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager resourceManager, ProfilerFiller profilerFiller, ProfilerFiller profilerFiller2, Executor executor, Executor executor2) {
        return this.listener.reload(preparationBarrier, resourceManager, profilerFiller, profilerFiller2, executor, executor2);
    }

    @Override
    public String toString() {
        return "PollinatedPreparableReloadListenerWrapper: " + this.listener;
    }
}
