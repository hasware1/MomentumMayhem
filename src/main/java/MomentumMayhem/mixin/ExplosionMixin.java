package MomentumMayhem.mixin;

import net.minecraft.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static MomentumMayhem.game.GameConfig.BREAKABLE_BLOCKS;

@Mixin(Block.class)
public abstract class ExplosionMixin {

    @Inject(method = "getBlastResistance", at = @At("HEAD"), cancellable = true)
    private void chaos$limitExplosionBreaking(CallbackInfoReturnable<Float> cir) {
        Block block = (Block) (Object) this;

        if (!BREAKABLE_BLOCKS.contains(block.getDefaultState().getBlock())) {
            cir.setReturnValue(3600000.0f);
        }
    }
}
