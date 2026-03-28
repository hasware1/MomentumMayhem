package MomentumMayhem.util;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Difficulty;
import net.minecraft.world.rule.GameRules;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static MomentumMayhem.game.GameConfig.*;
import static MomentumMayhem.game.GameManager.*;

public class HelperMethods {
    public static void sendTitle(ServerPlayerEntity player, String message, Formatting color) {
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal(message).formatted(color)));
    }
    public static void sendSound(ServerPlayerEntity player, SoundEvent sound) {
            player.networkHandler.sendPacket(
                    new PlaySoundS2CPacket(RegistryEntry.of(sound),
                            SoundCategory.MASTER, player.getX(), player.getY(), player.getZ(), 1.0f, 1.0f, 0L));
    }
    public static void clearAllEntities() {
        Box box = new Box(GROUND_MIN.toCenterPos(), ARENA_MAX.toCenterPos()).expand(5, 3, 5);
        List<Entity> entities = new ArrayList<>(getWorld().getOtherEntities(null, box));
        entities.stream().filter(e -> !(e instanceof PlayerEntity)).forEach(Entity::discard);
    }
    public static ServerPlayerEntity getPlayer(UUID uuid) {
        return getServer().getPlayerManager().getPlayer(uuid);
    }
    public static void resetArena(){
        List<BlockState> palette = List.of(
                Blocks.GRAY_WOOL.getDefaultState(),
                Blocks.CYAN_TERRACOTTA.getDefaultState(),
                Blocks.POLISHED_ANDESITE.getDefaultState(),
                Blocks.WHITE_WOOL.getDefaultState(),
                Blocks.LIGHT_GRAY_WOOL.getDefaultState()
        );
        for (BlockPos pos : BlockPos.iterate(GROUND_MIN, GROUND_MAX)) {
            BlockState randomState = palette.get(getWorld().random.nextInt(palette.size()));
            getWorld().setBlockState(pos, randomState, 2);
        }
        for (BlockPos pos : BlockPos.iterate(ARENA_MIN, ARENA_MAX)) {
            getWorld().setBlockState(pos, Blocks.AIR.getDefaultState(), 2);
        }
    }    public static void setRules(){
        getServer().setDifficulty(Difficulty.HARD, true);
        assert getWorld() != null;
        getWorld().getGameRules().setValue(GameRules.ADVANCE_TIME, false,getServer());
        getWorld().getGameRules().setValue(GameRules.DO_IMMEDIATE_RESPAWN, true, getServer());
        getWorld().getGameRules().setValue(GameRules.DO_MOB_SPAWNING, false, getServer());
        getWorld().getGameRules().setValue(GameRules.KEEP_INVENTORY, false, getServer());
        getWorld().getGameRules().setValue(GameRules.NATURAL_HEALTH_REGENERATION, false, getServer());
        getWorld().getGameRules().setValue(GameRules.ANNOUNCE_ADVANCEMENTS, false, getServer());
        getWorld().getGameRules().setValue(GameRules.DO_MOB_LOOT, false, getServer());
    }

    public static int map(int value, int inMin, int inMax, int outMin, int outMax) {
        return outMin + (outMax - outMin) * (value - inMin) / (inMax - inMin);
    }


}
