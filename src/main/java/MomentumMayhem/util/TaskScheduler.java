package MomentumMayhem.util;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

public class TaskScheduler {

    public static final List<ScheduledTask> tasks = new ArrayList<>();

    public static void init() {
        ServerTickEvents.START_SERVER_TICK.register(server -> tick());
    }

    public static ScheduledTask schedule(IntConsumer runnable, int delayTicks, int runs, boolean runFirst, Runnable onEnd) {
        if (runFirst){runs--;}
        ScheduledTask task = new ScheduledTask(runnable, delayTicks, runs,  runFirst, onEnd);
        tasks.add(task);
        if (runFirst){
            runnable.accept(0);
        }
        return task;
    }

    private static void tick() {
        List<ScheduledTask> tasksCopy = new ArrayList<>(tasks);

        for (ScheduledTask task : tasksCopy) {
            if (task.cancelled) {
                tasks.remove(task);
                continue;
            }
            task.ticksLeft--;

            if (task.ticksLeft <= 0) {
                task.runnable.accept(task.currentRun);

                task.ticksLeft = task.delayTicks;

                if (task.runs < 0 || task.currentRun < task.runs) {
                    task.currentRun++;
                } else {
                    tasks.remove(task);
                    if (task.onEnd != null) {
                        task.onEnd.run();
                    }
                }
            }
        }
    }
    public static void remove(ScheduledTask task) {
        if (task != null && tasks.contains(task)){
            task.cancelled = true;
            tasks.remove(task);
        }
    }

    public static class ScheduledTask {
        private final IntConsumer runnable;
        public int delayTicks;
        public int ticksLeft;
        public final int runs;
        public int currentRun;
        public boolean cancelled;
        public Runnable onEnd;
        public ScheduledTask(IntConsumer runnable, int delayTicks, int runs, boolean runFirst, Runnable onEnd) {
            this.runnable = runnable;
            this.delayTicks = delayTicks;
            this.ticksLeft = delayTicks;
            this.runs = runs;
            this.currentRun = 1;
            this.cancelled = false;
            this.onEnd = onEnd;
        }
    }
}
