package MomentumMayhem.game;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public class GameConfig {

    public static final int MIN_PLAYERS = 2;

    public static final BlockPos LOBBY_POS = new BlockPos(0, 88, 0);
    public static final BlockPos START_BUTTON = new BlockPos(0, 89, 2);
    public static final BlockPos ARENA_POS = new BlockPos(0, 77, 0);
    public static BlockPos GROUND_MIN = new BlockPos(-40, 64, -40);
    public static BlockPos GROUND_MAX = new BlockPos(40, 64, 40);
    public static final BlockPos ARENA_MIN = new BlockPos(-40, 65, -40);
    public static final BlockPos ARENA_MAX = new BlockPos(40, 86, 40);
    public static final int VOID_Y = 60;
    public static final int SPAWN_RADIUS = 10;
    public static final int MAX_TIME = 15 * 60 * 20;

    public static final List<Block> BREAKABLE_BLOCKS = List.of(
            Blocks.FIRE
    );
}