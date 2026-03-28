package MomentumMayhem.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public class ItemDespawnMixin {

    @Shadow
    private int itemAge;

    @Inject(method = "tick", at = @At("TAIL"))
    private void quickDespawn(CallbackInfo ci) {
        if (this.itemAge >= 40) {
            ItemEntity self = (ItemEntity) (Object) this;
            self.discard();
        }
    }
}