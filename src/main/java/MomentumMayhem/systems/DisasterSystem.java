package MomentumMayhem.systems;

import MomentumMayhem.game.GameManager;
import MomentumMayhem.util.TaskScheduler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.*;

import static MomentumMayhem.game.GameConfig.*;
import static MomentumMayhem.game.GameManager.*;
import static MomentumMayhem.util.HelperMethods.*;

public class DisasterSystem {
    private static TaskScheduler.ScheduledTask disasterTask;
    private static TaskScheduler.ScheduledTask delayTask;

    static Map<String, Runnable> disasters = new HashMap<>();

    static {
        disasters.put("Low Gravity", DisasterSystem::lowGravity);
        disasters.put("High Gravity", DisasterSystem::highGravity);
        disasters.put("Low Speed", DisasterSystem::lowSpeed);
        disasters.put("High Speed", DisasterSystem::highSpeed);
        disasters.put("Floor Swap", DisasterSystem::floorSwap);
    }


    public static void start() {
        stop();
        delayTask = TaskScheduler.schedule(x -> disasterTask = TaskScheduler.schedule(DisasterSystem::disasterSystemTick, 300, -1, true, null), 30 * 20, 1, false, null);
    }

    public static void stop() {
        TaskScheduler.remove(delayTask);
        TaskScheduler.remove(disasterTask);
    }

    public static void disasterSystemTick(int currentRun) {
        if (Math.random() < 0.5) {
            List<String> keys = new ArrayList<>(disasters.keySet());
            Collections.shuffle(keys);
            String name = keys.get(new Random().nextInt(keys.size()));
            countdownDisaster(name, disasters.get(name));
        }
    }

    public static void countdownDisaster(String name, Runnable disaster){
        TaskScheduler.schedule((int x) -> {
            for (UUID uuid: activePlayers) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player != null) {
                    if (x == 4) {
                        sendTitle(player, "⚠️ " + name + " ⚠️", Formatting.RED);
                        sendSound(player, SoundEvents.BLOCK_TRIAL_SPAWNER_OMINOUS_ACTIVATE);
                    } else if (x == 3) {
                        sendTitle(player, "3", Formatting.YELLOW);
                        sendSound(player, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value());
                    } else if (x == 2) {
                        sendTitle(player, "2", Formatting.GOLD);
                        sendSound(player, SoundEvents.BLOCK_NOTE_BLOCK_HAT.value());
                    } else if (x == 1) {
                        sendTitle(player, "1", Formatting.RED);
                        sendSound(player, SoundEvents.BLOCK_NOTE_BLOCK_PLING.value());
                    }
                }
            }
        }, 20, 5, true, () -> {
            disaster.run();

            GameManager.showDisaster(name);

            TaskScheduler.schedule((int y) -> {
                GameManager.clearDisaster();
            }, 10 * 20, 1, false, null);
        });
    }
    private static void lowGravity() {
        if (state != GameState.RUNNING) {
            return;
        }
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.JUMP_BOOST, 10 * 20, 4, false, false));
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOW_FALLING, 10 * 20, 0, false, false));
                player.sendMessage(Text.literal(" LOW GRAVITY! ")
                        .formatted(Formatting.AQUA, Formatting.BOLD), true);
                sendSound(player, SoundEvents.ENTITY_PLAYER_TELEPORT);
            }
        }
    }

    private static void highGravity() {
        if (state != GameState.RUNNING) {
            return;
        }
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.removeStatusEffect(StatusEffects.JUMP_BOOST);
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.MINING_FATIGUE, 10 * 20, 2, false, false));
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, 10 * 20, 0, false, false));
                player.sendMessage(Text.literal(" HIGH GRAVITY! ️")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                sendSound(player, SoundEvents.BLOCK_ANVIL_LAND);
            }
        }
    }

    private static void lowSpeed() {
        if (state != GameState.RUNNING) {
            return;
        }
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SLOWNESS, 10 * 20, 3, false, false));
                player.sendMessage(Text.literal(" LOW SPEED! ")
                        .formatted(Formatting.GRAY, Formatting.BOLD), true);
                sendSound(player, SoundEvents.ENTITY_TURTLE_EGG_HATCH);
            }
        }
    }

    private static void highSpeed() {
        if (state != GameState.RUNNING) {
            return;
        }
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.SPEED, 10 * 20, 3, false, false));
                player.sendMessage(Text.literal(" HIGH SPEED! ")
                        .formatted(Formatting.GREEN, Formatting.BOLD), true);
                sendSound(player, SoundEvents.ENTITY_HORSE_GALLOP);
            }
        }
    }

    private static void floorSwap() {
        if (state != GameState.RUNNING) {
            return;
        }

        List<Block> materials = List.of(
                Blocks.SLIME_BLOCK,
                Blocks.HONEY_BLOCK,
                Blocks.SOUL_SAND,
                Blocks.MAGMA_BLOCK,
                Blocks.COBWEB,
                Blocks.PACKED_ICE,
                Blocks.BLUE_ICE,
                Blocks.OBSIDIAN
        );

        Map<Block, String> materialNames = Map.of(
                Blocks.ICE, "Ice",
                Blocks.SLIME_BLOCK, "Slime",
                Blocks.HONEY_BLOCK, "Honey",
                Blocks.SOUL_SAND, "Soul Sand",
                Blocks.MAGMA_BLOCK, "Magma",
                Blocks.COBWEB, "Cobweb",
                Blocks.PACKED_ICE, "Packed Ice",
                Blocks.BLUE_ICE, "Blue Ice",
                Blocks.OBSIDIAN, "Obsidian"
        );

        Block randomMaterial = materials.get(new Random().nextInt(materials.size()));
        String materialName = materialNames.get(randomMaterial);

        int changedBlocks = 0;

        for (BlockPos pos : BlockPos.iterate(GROUND_MIN, GROUND_MAX)) {
            if (Math.random() < 0.4) {
                getWorld().setBlockState(pos, randomMaterial.getDefaultState(), 2);
                changedBlocks++;
            }
        }

        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.literal("FLOOR SWAP! Ground changed to " + materialName + "! ")
                        .formatted(Formatting.GOLD, Formatting.BOLD), true);
                sendSound(player, SoundEvents.BLOCK_STONE_BREAK);
            }
        }

        System.out.println("Floor Swap: Changed " + changedBlocks + " blocks to " + materialName);
    }
}