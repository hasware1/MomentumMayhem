package MomentumMayhem;

import MomentumMayhem.game.Events;
import MomentumMayhem.game.GameManager;
import MomentumMayhem.util.TaskScheduler;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MomentumMayhem implements ModInitializer {
	public static final String MOD_ID = "MomentumMayhem";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		TaskScheduler.init();
		Events.register();
		GameManager.init();
		LOGGER.info(MOD_ID + " has been initialized!");
	}
}