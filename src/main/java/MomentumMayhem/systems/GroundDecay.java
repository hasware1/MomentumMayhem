package MomentumMayhem.systems;

import MomentumMayhem.util.TaskScheduler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

import static MomentumMayhem.game.GameConfig.*;
import static MomentumMayhem.game.GameManager.*;
import static MomentumMayhem.util.HelperMethods.*;

public class GroundDecay {
    private static TaskScheduler.ScheduledTask decayTask;
    private static TaskScheduler.ScheduledTask delayTask;

    public static void start(){
        stop();
        delayTask = TaskScheduler.schedule(x->{
            for (UUID uuid : activePlayers){
                sendSound(getPlayer(uuid), SoundEvents.BLOCK_BEACON_POWER_SELECT);
                getPlayer(uuid).sendMessage(Text.literal("--------------------------").formatted(Formatting.DARK_RED, Formatting.BOLD));
                getPlayer(uuid).sendMessage(Text.literal("Ground Decay has started!!").formatted(Formatting.RED, Formatting.BOLD));
                getPlayer(uuid).sendMessage(Text.literal("--------------------------").formatted(Formatting.DARK_RED,  Formatting.BOLD));
            }
            decayTask = TaskScheduler.schedule(GroundDecay::decay, 6*20, 30, false, null);
        }, MAX_TIME - 300*20, 1, false, null);
    }
    public static void stop(){
        TaskScheduler.remove(delayTask);
        TaskScheduler.remove(decayTask);
    }

    public static void decay(int currentRun) {
        int index = map(currentRun, 1, 31, DECAY_MAX, DECAY_MIN);
        BlockState air = Blocks.AIR.getDefaultState();

        for (int i = -index; i <= index; i++) {
            for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
                BlockPos[] positions = {
                        new BlockPos(index, y, i),
                        new BlockPos(-index, y, i),
                        new BlockPos(i, y, index),
                        new BlockPos(-i, y, -index)
                };

                for (BlockPos pos : positions) {
                    if (!getWorld().getBlockState(pos).isAir()) {
                        getWorld().setBlockState(pos, air, 2 | 16);
                    }
                }
            }
        }
    }

}
