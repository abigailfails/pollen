package gg.moonflower.pollen.core.mixin;

import gg.moonflower.pollen.api.event.EventDispatcher;
import gg.moonflower.pollen.api.event.events.lifecycle.TickEvent;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void tickPre(CallbackInfo ci) {
        EventDispatcher.post(new TickEvent.EntityEvent.Pre((Entity) (Object) this));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickPost(CallbackInfo ci) {
        EventDispatcher.post(new TickEvent.EntityEvent.Post((Entity) (Object) this));
    }
}