package MomentumMayhem.game;

import MomentumMayhem.systems.DisasterSystem;
import MomentumMayhem.systems.ItemBuilder;
import MomentumMayhem.util.TaskScheduler;
import MomentumMayhem.util.TaskScheduler.ScheduledTask;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.DyedColorComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameMode;

import java.awt.*;
import java.util.*;
import java.util.List;

import static MomentumMayhem.game.GameConfig.*;
import static MomentumMayhem.systems.DisasterSystem.resetShrink;
import static MomentumMayhem.util.HelperMethods.*;

public class GameManager {

    public enum GameState {
        STARTING,
        WAITING,
        RUNNING,
        ENDING
    }

    private static MinecraftServer Server;
    private static ServerWorld World;
    public static MinecraftServer getServer() { return Server; }
    public static ServerWorld getWorld() { return World; }

    public static GameState state;

    public static Set<UUID> players = new HashSet<>();
    public static Set<UUID> activePlayers = new HashSet<>();

    private static ScheduledTask timeLimitTask;

    public static final ServerBossBar DisasterBar = new ServerBossBar(Text.literal("").formatted(Formatting.GOLD), BossBar.Color.YELLOW, BossBar.Style.PROGRESS);

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            state = GameState.WAITING;
            Server = server;
            World = server.getOverworld();
            setRules();
            resetArena();
        });
        ServerTickEvents.START_SERVER_TICK.register(GameManager::tick);
    }

    public static void tick(MinecraftServer server) {
        if (state == GameState.RUNNING) {
            if (activePlayers.size() <= 1) {
                endGame();
            }
            for (UUID uuid : activePlayers) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player.getY() < VOID_Y) {
                    player.damage(World, World.getDamageSources().outOfWorld(), 9999);
                }

                float healthPercent = player.getHealth() / player.getMaxHealth();
                int xpPoints = (int) (healthPercent * 100);
                player.setExperiencePoints(xpPoints);
                player.setExperienceLevel((int) Math.ceil(player.getHealth()));
            }
        }
    }

    public static void startGame() {
        state = GameState.RUNNING;

        for (UUID uuid : players) {
            ServerPlayerEntity player = getPlayer(uuid);
            if (player != null) {
                sendSound(player, SoundEvents.ENTITY_ENDER_DRAGON_GROWL);
                sendTitle(player, "MAYHEM BEGINS!", Formatting.RED);
            }
        }

        updateDisasterBar();
        resetArena();
        resetShrink();
        clearAllEntities();
        DisasterSystem.start();

        int i = 0;
        for (UUID uuid : players) {
            double angle = 2 * Math.PI * i / players.size();
            ServerPlayerEntity player = getPlayer(uuid);
            toArena(player, new BlockPos(
                    (int) (ARENA_POS.getX() + SPAWN_RADIUS * Math.cos(angle)),
                    ARENA_POS.getY(),
                    (int) (ARENA_POS.getZ() + SPAWN_RADIUS * Math.sin(angle))
            ));
            sendSound(player, SoundEvents.ENTITY_ENDER_DRAGON_AMBIENT);
            i++;
        }

        timeLimitTask = TaskScheduler.schedule(x -> endGame(), MAX_TIME, 1, false, null);
    }

    public static void endGame() {
        state = GameState.ENDING;
        TaskScheduler.remove(timeLimitTask);
        DisasterSystem.stop();
        clearAllEntities();

        if (!activePlayers.isEmpty()) {
            UUID winnerUUID = activePlayers.iterator().next();
            if (activePlayers.size() > 1) {
                float highest = getPlayer(winnerUUID).getHealth();
                for (UUID uuid : new ArrayList<>(activePlayers)) {
                    var player = getPlayer(uuid);
                    if (player.getHealth() > highest) {
                        winnerUUID = uuid;
                        highest = getPlayer(uuid).getHealth();
                    }
                    toLobby(player);
                    sendSound(player, SoundEvents.BLOCK_BEACON_DEACTIVATE);
                    player.sendMessage(Text.literal("Times Up!").formatted(Formatting.RED, Formatting.BOLD), true);
                    player.sendMessage(Text.literal("==========").formatted(Formatting.RED, Formatting.BOLD));
                    player.sendMessage(Text.literal("| Times Up! |").formatted(Formatting.RED, Formatting.BOLD));
                    player.sendMessage(Text.literal("==========").formatted(Formatting.RED, Formatting.BOLD));
                }
            } else {
                toLobby(getPlayer(winnerUUID));
            }

            ServerPlayerEntity winner = getPlayer(winnerUUID);
            sendSound(winner, SoundEvents.ENTITY_PLAYER_LEVELUP);
            sendTitle(winner, "YOU WON!", Formatting.GREEN);

            for (UUID uuid : players) {
                ServerPlayerEntity player = getPlayer(uuid);
                if (player != null && !player.getUuid().equals(winnerUUID)) {
                    sendSound(player, SoundEvents.ENTITY_WITHER_SPAWN);
                }
            }

            for (UUID uuid : players) {
                getPlayer(uuid).sendMessage(Text.literal("==========================").formatted(Formatting.YELLOW, Formatting.BOLD));
                getPlayer(uuid).sendMessage(Text.literal(winner.getName().getString()).formatted(Formatting.GREEN, Formatting.BOLD)
                        .append(Text.literal(" won the game!").formatted(Formatting.GOLD)));
                getPlayer(uuid).sendMessage(Text.literal("==========================").formatted(Formatting.YELLOW, Formatting.BOLD));
            }

            TaskScheduler.schedule((x) -> {}, 2 * 20, 1, false, null);
        }

        state = GameState.WAITING;
        updateDisasterBar();
    }

    public static void toArena(ServerPlayerEntity player, Vec3i pos) {
        activePlayers.add(player.getUuid());

        sendTitle(player, "FIGHT!", Formatting.RED);
        player.changeGameMode(GameMode.SURVIVAL);
        player.heal(20);
        player.getInventory().clear();
        player.teleport(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, false);
        player.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, Vec3d.of(ARENA_POS));

        int color = Color.HSBtoRGB((float) Math.random(), 0.9f, 0.9f);
        player.equipStack(EquipmentSlot.HEAD, new ItemBuilder(Items.LEATHER_HELMET, 1).maxDura(30).withComponent(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color)).build());
        player.equipStack(EquipmentSlot.CHEST, new ItemBuilder(Items.LEATHER_CHESTPLATE, 1).maxDura(30).withComponent(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color)).build());
        player.equipStack(EquipmentSlot.LEGS, new ItemBuilder(Items.LEATHER_LEGGINGS, 1).maxDura(30).withComponent(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color)).build());
        player.equipStack(EquipmentSlot.FEET, new ItemBuilder(Items.LEATHER_BOOTS, 1).maxDura(30).withComponent(DataComponentTypes.DYED_COLOR, new DyedColorComponent(color)).build());

        List<String> names = List.of(
                "Yeet", "Bonk", "Boop", "WACK", "Fling",
                "YEETUS", "Bye Bye", "See Ya", "Vroom",
                "Zoom", "Bam", "Pow", "Woosh", "Fwoosh",
                "Adios", "Cya", "Boing", "Swoosh", "Yeetus Deletus"
        );

        String randomName = names.get(new Random().nextInt(names.size()));

        player.equipStack(EquipmentSlot.MAINHAND, new ItemBuilder(Items.STICK, 1)
                .name(randomName, Formatting.GREEN)
                .withEnchant(Enchantments.KNOCKBACK, 3)
                .maxDura(30)
                .build());
    }

    public static void toLobby(ServerPlayerEntity player) {
        activePlayers.remove(player.getUuid());
        player.sendMessage(Text.literal(""), true);
        player.heal(20);
        player.clearStatusEffects();
        player.addStatusEffect(new StatusEffectInstance(StatusEffects.SATURATION, -1, 0, false, false));
        player.setFireTicks(0);
        player.teleport(LOBBY_POS.getX() + 0.5, LOBBY_POS.getY(), LOBBY_POS.getZ() + 0.5, false);
        player.getInventory().clear();
        player.changeGameMode(GameMode.SURVIVAL);
    }

    public static void updateDisasterBar() {
        DisasterBar.clearPlayers();
        for (UUID uuid : players) {
            if (state == GameState.RUNNING) {
                DisasterBar.addPlayer(getPlayer(uuid));
            } else {
                DisasterBar.removePlayer(getPlayer(uuid));
            }
        }
    }

    public static void showDisaster(String disasterName) {
        DisasterBar.setName(Text.literal(disasterName).formatted(Formatting.GOLD, Formatting.BOLD));
    }

    public static void clearDisaster() {
        DisasterBar.setName(Text.literal(""));
    }
}