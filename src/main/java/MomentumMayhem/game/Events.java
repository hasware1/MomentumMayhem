package MomentumMayhem.game;

import MomentumMayhem.util.TaskScheduler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;

import java.util.UUID;

import static MomentumMayhem.game.GameConfig.BREAKABLE_BLOCKS;
import static MomentumMayhem.game.GameConfig.MIN_PLAYERS;
import static MomentumMayhem.game.GameManager.*;
import static MomentumMayhem.util.HelperMethods.*;

public class Events {
    public static void register() {

        ServerPlayerEvents.JOIN.register((player) -> {
            players.add(player.getUuid());
            updateDisasterBar();
            toLobby(player);
            sendTitle(player, "Welcome to Momentum Mayhem!", Formatting.GOLD);
        });

        ServerPlayerEvents.LEAVE.register(player -> {
            if (player.hasVehicle()) {
                Entity vehicle = player.getVehicle();
                player.stopRiding();
                if (vehicle != null) {
                    vehicle.discard();
                }
            }
            players.remove(player.getUuid());
            activePlayers.remove(player.getUuid());
        });

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity) {
                for (UUID uuid : players) {
                    sendSound(getPlayer(uuid), SoundEvents.ENTITY_WITHER_SPAWN);
                }
            }
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldplayer, newplayer, alive) -> {
            toLobby(newplayer);
            sendTitle(newplayer, "You Died", Formatting.RED);
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayerEntity player && !source.isOf(DamageTypes.GENERIC_KILL)) {
                if (state == GameState.RUNNING) {
                    return activePlayers.contains(player.getUuid());
                } else {
                    return false;
                }
            }
            return true;
        });

        UseBlockCallback.EVENT.register(((player, world, hand, hitResult) -> {
            if (hand != Hand.MAIN_HAND || world.isClient()) {
                return ActionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            if (pos.equals(GameConfig.START_BUTTON) && world.getBlockState(pos).getBlock() == Blocks.WARPED_BUTTON && !world.getBlockState(pos).get(Properties.POWERED)) {
                ServerWorld serverWorld = (ServerWorld) world;
                serverWorld.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.3, 0.3, 0.3, 0.05);

                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;

                if (state != GameState.WAITING) {
                    sendTitle(serverPlayer, "GAME ALREADY RUNNING!", Formatting.GREEN);
                    sendSound(serverPlayer, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
                } else if (players.size() < MIN_PLAYERS) {
                    sendTitle(serverPlayer, "NOT ENOUGH PLAYERS!", Formatting.RED);
                    sendSound(serverPlayer, SoundEvents.BLOCK_NOTE_BLOCK_BASS.value());
                    serverPlayer.sendMessage(Text.literal("Need " + MIN_PLAYERS + " players. Current: " + players.size())
                            .formatted(Formatting.YELLOW), true);
                } else {
                    state = GameState.STARTING;
                    TaskScheduler.schedule((int x) -> {
                        for (UUID uuid : players) {
                            if (x != 3) {
                                sendTitle(getPlayer(uuid), String.valueOf(3 - x), Formatting.YELLOW);
                                sendSound(getPlayer(uuid), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value());
                            }
                        }
                    }, 20, 4, true, GameManager::startGame);
                }
            }
            return ActionResult.PASS;
        }));

        PlayerBlockBreakEvents.BEFORE.register((world, player, blockPos, state, entity) -> {
            if (player.getGameMode() == GameMode.CREATIVE) {
                return true;
            }
            return BREAKABLE_BLOCKS.contains(state.getBlock());
        });
    }
}