package gg.moonflower.pollen.api.registry;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import dev.architectury.injectables.annotations.ExpectPlatform;
import gg.moonflower.pollen.api.platform.Platform;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * An abstracted registry for wrapping platform-specific registries.
 *
 * @param <T> The object type
 * @author Jackson
 * @since 1.0.0
 */
public abstract class PollinatedRegistry<T> implements Codec<T>, Keyable {

    protected final String modId;
    private boolean registered;

    protected PollinatedRegistry(String modId) {
        this.modId = modId;
    }

    /**
     * Creates an {@link PollinatedRegistry} backed by a platform-specific registry.
     * <p>Forge users: If there's no ForgeRegistry for the object type, this will return a {@link PollinatedRegistry.VanillaImpl}.
     *
     * @param registry The registry to register objects to.
     * @param modId    The mod id to register to.
     * @param <T>      The registry type.
     * @return A {@link PollinatedRegistry} backed by a platform-specific registry.
     */
    @ExpectPlatform
    public static <T> PollinatedRegistry<T> create(Registry<T> registry, String modId) {
        return Platform.error();
    }

    /**
     * Creates an {@link PollinatedRegistry} backed by a platform-specific registry. This should only be used to register to another mod's registry.
     *
     * @param registry The registry to register objects to.
     * @param modId    The mod id to register to.
     * @param <T>      The registry type.
     * @return A {@link PollinatedRegistry} backed by a platform-specific registry.
     */
    @ExpectPlatform
    public static <T> PollinatedRegistry<T> create(PollinatedRegistry<T> registry, String modId) {
        return Platform.error();
    }

    /**
     * Creates an {@link PollinatedRegistry} backed by a {@link Registry}.
     * <p>Users should always use {@link PollinatedRegistry#create(Registry, String)}.
     * <p>This is for very specific cases where vanilla registries must strictly be used and {@link PollinatedRegistry#create(Registry, String)} can't do what you need.
     *
     * @param registry The registry to register objects to.
     * @param modId    The mod id to register to.
     * @param <T>      The registry type.
     * @return A {@link PollinatedRegistry} backed by a {@link Registry}.
     */
    public static <T> PollinatedRegistry<T> createVanilla(Registry<T> registry, String modId) {
        return new VanillaImpl<>(registry, modId);
    }

    /**
     * Creates a new simple registry
     *
     * @param registryId The registry {@link ResourceLocation} used as the registry id
     * @param <T>        The type stored in the Registry
     * @return An instance of FabricRegistryBuilder
     */
    @ExpectPlatform
    public static <T> PollinatedRegistry<T> createSimple(Class<T> type, ResourceLocation registryId) {
        return Platform.error();
    }

    /**
     * @param registryId The registry {@link ResourceLocation} used as the registry id
     * @param defaultId  The default registry id
     * @param <T>        The type stored in the Registry
     * @return An instance of FabricRegistryBuilder
     */
    @ExpectPlatform
    public static <T> PollinatedRegistry<T> createDefaulted(Class<T> type, ResourceLocation registryId, ResourceLocation defaultId) {
        return Platform.error();
    }

    /**
     * Registers an object.
     *
     * @param id     The id of the object.
     * @param object The object to register.
     * @param <R>    The registry type.
     * @return The registered object in a {@link Supplier}.
     */
    public abstract <R extends T> Supplier<R> register(String id, Supplier<R> object);

    /**
     * Registers an object or a dummy object based on a condition.
     *
     * @param id       The id of the object.
     * @param dummy    The object to register if the condition is false.
     * @param object   The object to register if the condition is true.
     * @param register Whether the object should be registered or the dummy should be registered.
     * @param <R>      The registry type.
     * @return The registered object in a {@link Supplier}
     */
    public <R extends T> Supplier<R> registerConditional(String id, Supplier<R> dummy, Supplier<R> object, boolean register) {
        return this.register(id, register ? object : dummy);
    }

    /**
     * Initializes the registry for a {@link Platform}.
     *
     * @param mod The {@link Platform} to register the registry onto.
     * @throws IllegalStateException if the registry has already been registered.
     */
    public final void register(Platform mod) {
        if (this.registered)
            throw new IllegalStateException("Cannot register a PollinatedRegistry twice!");
        this.registered = true;
        this.onRegister(mod);
    }

    @ApiStatus.OverrideOnly
    protected void onRegister(Platform mod) {
    }

    @ApiStatus.Internal
    public static class VanillaImpl<T> extends PollinatedRegistry<T> {

        private final Registry<T> registry;

        private VanillaImpl(Registry<T> registry, String modId) {
            super(modId);
            this.registry = registry;
        }

        @Override
        public <R extends T> Supplier<R> register(String id, Supplier<R> object) {
            R registered = Registry.register(this.registry, new ResourceLocation(this.modId, id), object.get());
            return () -> registered;
        }

        @Override
        public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
            return this.registry.decode(ops, input);
        }

        @Override
        public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
            return this.registry.encode(input, ops, prefix);
        }

        @Override
        public <T1> Stream<T1> keys(DynamicOps<T1> ops) {
            return this.registry.keys(ops);
        }

        public Registry<T> getRegistry() {
            return registry;
        }
    }
}
