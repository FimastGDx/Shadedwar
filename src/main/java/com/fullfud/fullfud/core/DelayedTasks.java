package com.fullfud.fullfud.core;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs a {@link Runnable} a fixed number of ticks later, on the server thread.
 *
 * <p>Two features need this and neither wants its own tick listener: the post-blast light refresh has to
 * wait for the explosion to finish writing blocks before it can re-send anything, and reconnecting to an
 * FPV drone has to wait for a chunk ticket to actually produce a chunk.
 *
 * <p>Tasks are keyed to a level and fire from that level's tick, so a task never runs against a world
 * that has gone away — and if the world unloads first, the task is simply dropped.
 */
public final class DelayedTasks {

    private record Task(ServerLevel level, long dueTick, Runnable action) {
    }

    /** Only ever touched from the server thread. */
    private static final List<Task> TASKS = new ArrayList<>();

    private DelayedTasks() {
    }

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(DelayedTasks::onEndWorldTick);
        ServerWorldEvents.UNLOAD.register((server, level) -> TASKS.removeIf(task -> task.level() == level));
    }

    public static void schedule(final ServerLevel level, final int delayTicks, final Runnable action) {
        if (level == null || action == null) {
            return;
        }
        TASKS.add(new Task(level, level.getGameTime() + Math.max(1, delayTicks), action));
    }

    /**
     * Collects everything due for this level, <em>then</em> runs it.
     *
     * <p>The two steps cannot be merged. A task is free to schedule another task — the FPV recall retries
     * itself that way — and running one while the iterator is still open turned the follow-up
     * {@code TASKS.add} into a structural modification, i.e. a {@link java.util.ConcurrentModificationException}
     * on the next {@code next()} and a dead world tick. Since {@link #schedule} clamps the delay to at least
     * one tick, nothing added by this drain can be due before the next tick anyway.
     */
    private static void onEndWorldTick(final ServerLevel level) {
        if (TASKS.isEmpty()) {
            return;
        }
        final long now = level.getGameTime();
        List<Task> due = null;
        for (final Iterator<Task> iterator = TASKS.iterator(); iterator.hasNext(); ) {
            final Task task = iterator.next();
            if (task.level() != level || task.dueTick() > now) {
                continue;
            }
            iterator.remove();
            if (due == null) {
                due = new ArrayList<>(4);
            }
            due.add(task);
        }
        if (due == null) {
            return;
        }
        for (final Task task : due) {
            try {
                task.action().run();
            } catch (RuntimeException ignored) {
                // A deferred task is always a best-effort touch-up; never take the server tick down with it.
            }
        }
    }
}
