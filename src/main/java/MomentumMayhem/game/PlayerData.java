package MomentumMayhem.game;

import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerData {
    public Text message = Text.empty();
    public List<String> messages = new ArrayList<>(Arrays.asList("", "", ""));
}
