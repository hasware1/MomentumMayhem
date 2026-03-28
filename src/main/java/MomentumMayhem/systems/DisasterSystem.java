package MomentumMayhem.systems;

import MomentumMayhem.game.GameManager;
import MomentumMayhem.util.TaskScheduler;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
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
        disasters.put("Shrinking Arena", DisasterSystem::shrinkGround);
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
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                // Set a custom gravity value (lower = less gravity)
                player.setNoGravity(true);

                // Schedule a task to restore normal gravity
                TaskScheduler.schedule((int x) -> {
                    player.setNoGravity(false);
                }, 10 * 20, 1, false, null);

                player.sendMessage(Text.literal("☁️ LOW GRAVITY! ☁️")
                        .formatted(Formatting.AQUA, Formatting.BOLD), true);
                sendSound(player, SoundEvents.ENTITY_PLAYER_TELEPORT);
            }
        }
    }

    private static void highGravity() {
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                // Apply downward velocity every tick during the disaster
                TaskScheduler.schedule((int x) -> {
                    if (state == GameState.RUNNING && activePlayers.contains(uuid)) {
                        player.addVelocity(0, -0.3, 0);
                    }
                }, 0, 10 * 20, true, null);

                player.sendMessage(Text.literal(" HIGH GRAVITY! ")
                        .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                sendSound(player, SoundEvents.BLOCK_ANVIL_LAND);
            }
        }
    }

    private static void lowSpeed() {
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                var speedAttribute = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (speedAttribute != null) {
                    // Create the modifier
                    EntityAttributeModifier modifier = new EntityAttributeModifier(
                            Identifier.of("momentum_mayhem", "low_speed"),
                            -0.7,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    );

                    // Apply the modifier
                    speedAttribute.addTemporaryModifier(modifier);

                    // Remove after 10 seconds by passing the modifier directly
                    TaskScheduler.schedule((int x) -> {
                        speedAttribute.removeModifier(modifier);
                    }, 10 * 20, 1, false, null);
                }

                player.sendMessage(Text.literal(" LOW SPEED! ")
                        .formatted(Formatting.GRAY, Formatting.BOLD), true);
                sendSound(player, SoundEvents.ENTITY_TURTLE_EGG_HATCH);
            }
        }
    }
    private static void highSpeed() {
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                var speedAttribute = player.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
                if (speedAttribute != null) {
                    EntityAttributeModifier modifier = new EntityAttributeModifier(
                            Identifier.of("momentum_mayhem", "high_speed"),
                            0.7,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    );

                    speedAttribute.addTemporaryModifier(modifier);

                    TaskScheduler.schedule((int x) -> {
                        speedAttribute.removeModifier(modifier);
                    }, 10 * 20, 1, false, null);
                }

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
    private static int shrinkCount = 0;
    private static int currentMinX, currentMaxX, currentMinZ, currentMaxZ;

    private static void shrinkGround() {
        if (state != GameState.RUNNING) return;

        // Initialize current bounds on first shrink
        if (shrinkCount == 0) {
            currentMinX = GROUND_MIN.getX();
            currentMaxX = GROUND_MAX.getX();
            currentMinZ = GROUND_MIN.getZ();
            currentMaxZ = GROUND_MAX.getZ();
        }

        shrinkCount++;

        // Shrink by 1 block from each side
        int newMinX = currentMinX + 1;
        int newMaxX = currentMaxX - 1;
        int newMinZ = currentMinZ + 1;
        int newMaxZ = currentMaxZ - 1;

        // Check if ground is too small
        if (newMaxX - newMinX < 3 || newMaxZ - newMinZ < 3) {
            for (UUID uuid : activePlayers) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player != null) {
                    player.sendMessage(Text.literal("SUDDEN DEATH! Final stand!")
                            .formatted(Formatting.DARK_RED, Formatting.BOLD), true);
                    sendSound(player, SoundEvents.ENTITY_WITHER_SPAWN);
                }
            }
            return;
        }

        // Remove the outer ring of ground blocks
        for (int x = currentMinX; x <= currentMaxX; x++) {
            for (int z = currentMinZ; z <= currentMaxZ; z++) {
                // Check if this block is on the border
                boolean isOnBorder = x == currentMinX || x == currentMaxX ||
                        z == currentMinZ || z == currentMaxZ;

                if (isOnBorder) {
                    // Remove blocks at ground level
                    for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!getWorld().getBlockState(pos).isAir()) {
                            getWorld().setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        // Update current bounds
        currentMinX = newMinX;
        currentMaxX = newMaxX;
        currentMinZ = newMinZ;
        currentMaxZ = newMaxZ;

        // Add a warning border (red glass) to show new edge
        for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
            // Draw border on new edges
            for (int x = currentMinX; x <= currentMaxX; x++) {
                BlockPos frontEdge = new BlockPos(x, y, currentMinZ);
                BlockPos backEdge = new BlockPos(x, y, currentMaxZ);
                if (getWorld().getBlockState(frontEdge).isAir()) {
                    getWorld().setBlockState(frontEdge, Blocks.RED_STAINED_GLASS.getDefaultState(), 2);
                }
                if (getWorld().getBlockState(backEdge).isAir()) {
                    getWorld().setBlockState(backEdge, Blocks.RED_STAINED_GLASS.getDefaultState(), 2);
                }
            }

            for (int z = currentMinZ; z <= currentMaxZ; z++) {
                BlockPos leftEdge = new BlockPos(currentMinX, y, z);
                BlockPos rightEdge = new BlockPos(currentMaxX, y, z);
                if (getWorld().getBlockState(leftEdge).isAir()) {
                    getWorld().setBlockState(leftEdge, Blocks.RED_STAINED_GLASS.getDefaultState(), 2);
                }
                if (getWorld().getBlockState(rightEdge).isAir()) {
                    getWorld().setBlockState(rightEdge, Blocks.RED_STAINED_GLASS.getDefaultState(), 2);
                }
            }
        }

        // Send warning to players
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.sendMessage(Text.literal("GROUND SHRINKING! " + shrinkCount + " layers removed!")
                        .formatted(Formatting.RED, Formatting.BOLD), true);
                sendSound(player, SoundEvents.BLOCK_ANVIL_LAND);
            }
        }

        System.out.println("Ground shrunk to: X[" + currentMinX + " to " + currentMaxX +
                "] Z[" + currentMinZ + " to " + currentMaxZ + "]");
    }
    public static void resetShrink() {
        shrinkCount = 0;
        currentMinX = GROUND_MIN.getX();
        currentMaxX = GROUND_MAX.getX();
        currentMinZ = GROUND_MIN.getZ();
        currentMaxZ = GROUND_MAX.getZ();
    }
}
