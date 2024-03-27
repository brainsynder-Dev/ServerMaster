/*
 * Copyright © 2023
 * BSDevelopment <https://bsdevelopment.org>
 */

package org.bsdevelopment.servermaster.utils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ScheduledFuture {
    private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(4);

    public static void main(String[] args) {
        Supplier<CompletableFuture<Integer>> asyncTask = () -> CompletableFuture.completedFuture(10 + 25);
        CompletableFuture<Integer> future = ScheduledFuture.scheduleAsync(asyncTask, 1, TimeUnit.SECONDS);
        future.thenAccept(System.out::println);
    }

    public static void runRepeatTask (Runnable runnable, int delay, TimeUnit unit) {
        Supplier<CompletableFuture<Runnable>> asyncTask = () -> CompletableFuture.completedFuture(() -> {
            runnable.run();
            runRepeatTask(runnable, delay, unit);
        });
        CompletableFuture<Runnable> future = ScheduledFuture.scheduleAsync(asyncTask, delay, unit);
        future.thenAccept(Runnable::run);
    }

    public static void runDelayedTask (Runnable runnable, int delay, TimeUnit unit) {
        Supplier<CompletableFuture<Runnable>> asyncTask = () -> CompletableFuture.completedFuture(runnable);
        CompletableFuture<Runnable> future = ScheduledFuture.scheduleAsync(asyncTask, delay, unit);
        future.thenAccept(Runnable::run);
    }

    public static <T> CompletableFuture<T> schedule(Supplier<T> command, long delay, TimeUnit unit) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        EXECUTOR.schedule((() -> {
                    try {
                        return completableFuture.complete(command.get());
                    } catch (Throwable t) {
                        return completableFuture.completeExceptionally(t);
                    }
                }), delay, unit
        );
        return completableFuture;
    }

    public static <T> CompletableFuture<T> scheduleAsync(Supplier<CompletableFuture<T>> command, long delay, TimeUnit unit) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        EXECUTOR.schedule((() -> {
                    command.get().thenAccept(completableFuture::complete)
                            .exceptionally(
                                    t -> {
                                        completableFuture.completeExceptionally(t);
                                        return null;
                                    }
                            );
                }), delay, unit
        );
        return completableFuture;
    }
}
