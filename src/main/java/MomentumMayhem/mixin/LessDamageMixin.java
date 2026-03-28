package MomentumMayhem.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerEntity.class)
public class LessDamageMixin {
    @ModifyVariable(
            method = "damage",
            at = @At("HEAD"),
            argsOnly = true
    )
    private float reduceDamage(float amount) {
        return amount * 0.05f;
    }
}
