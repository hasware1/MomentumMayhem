package MomentumMayhem.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public class AutoIgniteTNT {

    @Inject(method = "onPlaced", at = @At("HEAD"))
    private void chaos$instantPrimeTnt(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack, CallbackInfo ci) {
        if (state.getBlock() instanceof TntBlock) {
            if (!world.isClient()) {
                TntEntity tnt = new TntEntity(world, (double)pos.getX() + (double)0.5F, pos.getY(), (double)pos.getZ() + (double)0.5F, placer);
                tnt.setFuse(40);
                world.spawnEntity(tnt);
                world.emitGameEvent(placer, GameEvent.PRIME_FUSE, pos);
                world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0F, 1.0F);
                world.removeBlock(pos, false);
            }
        }
    }
}