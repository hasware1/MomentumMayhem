package MomentumMayhem.systems;

import MomentumMayhem.game.GameManager;
import MomentumMayhem.util.TaskScheduler;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
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

    public static void countdownDisaster(String name, Runnable disaster) {
        TaskScheduler.schedule((int x) -> {
            for (UUID uuid : activePlayers) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player != null) {
                    if (x == 4) {
                        sendTitle(player, " " + name + " ", Formatting.RED);
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
            TaskScheduler.schedule((int y) -> GameManager.clearDisaster(), 20 * 20, 1, false, null);
        });
    }

    private static void lowGravity() {
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                player.setNoGravity(true);

                TaskScheduler.schedule((int x) -> {
                    if (state == GameState.RUNNING && activePlayers.contains(uuid)) {
                        player.addVelocity(0, 0.15, 0);
                    }
                }, 0, 20 * 20, true, null);

                TaskScheduler.schedule((int x) -> player.setNoGravity(false), 20 * 20, 1, false, null);

                getWorld().spawnParticles(ParticleTypes.CLOUD,
                        player.getX(), player.getY() + 0.5, player.getZ(),
                        40, 0.8, 1.5, 0.8, 0.1);

                sendTitle(player, "LOW GRAVITY", Formatting.AQUA);
                sendSound(player, SoundEvents.ENTITY_PLAYER_TELEPORT);
            }
        }
    }

    private static void highGravity() {
        if (state != GameState.RUNNING) return;
        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                TaskScheduler.schedule((int x) -> {
                    if (state == GameState.RUNNING && activePlayers.contains(uuid)) {
                        player.addVelocity(0, -0.7, 0);
                    }
                }, 0, 20 * 20, true, null);

                var jumpAttribute = player.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
                if (jumpAttribute != null) {
                    EntityAttributeModifier jumpModifier = new EntityAttributeModifier(
                            Identifier.of("MomentumMayhem", "high_gravity_jump"),
                            -0.6,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    );
                    jumpAttribute.addTemporaryModifier(jumpModifier);
                    TaskScheduler.schedule((int x) -> jumpAttribute.removeModifier(jumpModifier), 20 * 20, 1, false, null);
                }

                getWorld().spawnParticles(new BlockStateParticleEffect(ParticleTypes.FALLING_DUST, Blocks.ANVIL.getDefaultState()),
                        player.getX(), player.getY() + 1.0, player.getZ(),
                        30, 0.5, 1.0, 0.5, 0.15);

                sendTitle(player, "HIGH GRAVITY", Formatting.DARK_RED);
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
                    EntityAttributeModifier modifier = new EntityAttributeModifier(
                            Identifier.of("MomentumMayhem", "low_speed"),
                            -0.85,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    );
                    speedAttribute.addTemporaryModifier(modifier);
                    TaskScheduler.schedule((int x) -> speedAttribute.removeModifier(modifier), 20 * 20, 1, false, null);
                }

                getWorld().spawnParticles(ParticleTypes.ITEM_COBWEB,
                        player.getX(), player.getY() + 0.2, player.getZ(),
                        25, 0.5, 0.2, 0.5, 0.05);

                sendTitle(player, "LOW SPEED", Formatting.GRAY);
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
                            Identifier.of("MomentumMayhem", "high_speed"),
                            1.2,
                            EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    );
                    speedAttribute.addTemporaryModifier(modifier);
                    TaskScheduler.schedule((int x) -> speedAttribute.removeModifier(modifier), 20 * 20, 1, false, null);
                }

                getWorld().spawnParticles(ParticleTypes.SWEEP_ATTACK,
                        player.getX() - player.getVelocity().x * 2, player.getY() + 0.5, player.getZ() - player.getVelocity().z * 2,
                        8, 0.3, 0.3, 0.3, 0);

                sendTitle(player, "HIGH SPEED", Formatting.GREEN);
                sendSound(player, SoundEvents.ENTITY_HORSE_GALLOP);
            }
        }
    }

    private static void floorSwap() {
        if (state != GameState.RUNNING) return;

        List<Block> materials = List.of(
                Blocks.SLIME_BLOCK,
                Blocks.HONEY_BLOCK,
                Blocks.SOUL_SAND,
                Blocks.MAGMA_BLOCK,
                Blocks.PACKED_ICE,
                Blocks.BLUE_ICE,
                Blocks.OBSIDIAN,
                Blocks.TNT,
                Blocks.COBWEB
        );

        Map<Block, String> materialNames = Map.of(
                Blocks.SLIME_BLOCK, "Slime",
                Blocks.HONEY_BLOCK, "Honey",
                Blocks.SOUL_SAND, "Soul Sand",
                Blocks.MAGMA_BLOCK, "Magma",
                Blocks.PACKED_ICE, "Packed Ice",
                Blocks.BLUE_ICE, "Blue Ice",
                Blocks.OBSIDIAN, "Obsidian",
                Blocks.TNT, "TNT",
                Blocks.COBWEB, "Cobweb"
        );

        Block randomMaterial = materials.get(new Random().nextInt(materials.size()));
        String materialName = materialNames.get(randomMaterial);

        int changedBlocks = 0;

        for (BlockPos pos : BlockPos.iterate(GROUND_MIN, GROUND_MAX)) {
            if (Math.random() < 0.6) {
                getWorld().setBlockState(pos, randomMaterial.getDefaultState(), 2);
                changedBlocks++;
            }
        }

        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                sendTitle(player, "FLOOR SWAP: " + materialName, Formatting.GOLD);
                sendSound(player, SoundEvents.BLOCK_STONE_BREAK);
            }
        }

        System.out.println("Floor Swap: Changed " + changedBlocks + " blocks to " + materialName);
    }

    private static int shrinkCount = 0;
    private static int currentMinX, currentMaxX, currentMinZ, currentMaxZ;

    private static void shrinkGround() {
        if (state != GameState.RUNNING) return;

        if (shrinkCount == 0) {
            currentMinX = GROUND_MIN.getX();
            currentMaxX = GROUND_MAX.getX();
            currentMinZ = GROUND_MIN.getZ();
            currentMaxZ = GROUND_MAX.getZ();
        }

        shrinkCount++;

        int newMinX = currentMinX + 2;
        int newMaxX = currentMaxX - 2;
        int newMinZ = currentMinZ + 2;
        int newMaxZ = currentMaxZ - 2;

        if (newMaxX - newMinX < 5 || newMaxZ - newMinZ < 5) {
            for (UUID uuid : activePlayers) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player != null) {
                    sendTitle(player, "SUDDEN DEATH", Formatting.DARK_RED);
                    sendSound(player, SoundEvents.ENTITY_WITHER_SPAWN);
                }
            }
            return;
        }

        for (int x = currentMinX; x <= currentMaxX; x++) {
            for (int z = currentMinZ; z <= currentMaxZ; z++) {
                boolean isOnBorder = x == currentMinX || x == currentMaxX || z == currentMinZ || z == currentMaxZ;

                if (isOnBorder) {
                    for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!getWorld().getBlockState(pos).isAir()) {
                            getWorld().setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
            }
        }

        currentMinX = newMinX;
        currentMaxX = newMaxX;
        currentMinZ = newMinZ;
        currentMaxZ = newMaxZ;

        GROUND_MIN = new BlockPos(currentMinX, GROUND_MIN.getY(), currentMinZ);
        GROUND_MAX = new BlockPos(currentMaxX, GROUND_MAX.getY(), currentMaxZ);

        for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
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

        for (int y = GROUND_MIN.getY(); y <= GROUND_MAX.getY(); y++) {
            for (int z = currentMinZ; z <= currentMaxZ; z++) {
                BlockPos leftEdge = new BlockPos(currentMinX, y, z);
                BlockPos rightEdge = new BlockPos(currentMaxX, y, z);
                getWorld().spawnParticles(ParticleTypes.LAVA,
                        leftEdge.getX() + 0.5, leftEdge.getY() + 0.5, leftEdge.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.05);
                getWorld().spawnParticles(ParticleTypes.LAVA,
                        rightEdge.getX() + 0.5, rightEdge.getY() + 0.5, rightEdge.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.05);
            }

            for (int x = currentMinX; x <= currentMaxX; x++) {
                BlockPos frontEdge = new BlockPos(x, y, currentMinZ);
                BlockPos backEdge = new BlockPos(x, y, currentMaxZ);
                getWorld().spawnParticles(ParticleTypes.LAVA,
                        frontEdge.getX() + 0.5, frontEdge.getY() + 0.5, frontEdge.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.05);
                getWorld().spawnParticles(ParticleTypes.LAVA,
                        backEdge.getX() + 0.5, backEdge.getY() + 0.5, backEdge.getZ() + 0.5,
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        }

        for (UUID uuid : activePlayers) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                sendTitle(player, "GROUND SHRINKING", Formatting.RED);
                sendSound(player, SoundEvents.BLOCK_ANVIL_LAND);
            }
        }

        System.out.println("Ground shrunk to: X[" + currentMinX + " to " + currentMaxX +
                "] Z[" + currentMinZ + " to " + currentMaxZ + "]");
    }

    public static void resetShrink() {
        shrinkCount = 0;
        currentMinX = -40;
        currentMaxX = 40;
        currentMinZ = -40;
        currentMaxZ = 40;

        GROUND_MIN = new BlockPos(-40, 64, -40);
        GROUND_MAX = new BlockPos(40, 64, 40);
    }
}