package de.dytanic.cloudnet.common.concurrent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.*;

public class CountingTask<V> implements ITask<V> {

    private final V value;
    private final CompletableFuture<V> future = new CompletableFuture<>();
    private final Collection<ITaskListener<V>> listeners = new ArrayList<>();
    private int count;

    public CountingTask(V value, int initialCount) {
        this.value = value;
        this.count = initialCount;
    }

    public void incrementCount() {
        ++this.count;
    }

    public void countDown() {
        --this.count;
        if (this.count <= 0) {
            for (ITaskListener<V> listener : this.listeners) {
                listener.onComplete(this, this.value);
            }
            this.future.complete(this.value);
        }
    }

    @Override
    public @NotNull ITask<V> addListener(ITaskListener<V> listener) {
        this.listeners.add(listener);
        return this;
    }

    @Override
    public @NotNull ITask<V> clearListeners() {
        this.listeners.clear();
        return this;
    }

    @Override
    public Collection<ITaskListener<V>> getListeners() {
        return this.listeners;
    }

    @Override
    public Callable<V> getCallable() {
        return () -> this.value;
    }

    @Override
    public V getDef(V def) {
        return this.get(5, TimeUnit.SECONDS, def);
    }

    @Override
    public V get(long time, TimeUnit timeUnit, V def) {
        try {
            return this.future.get(time, timeUnit);
        } catch (InterruptedException | ExecutionException | TimeoutException exception) {
            return def;
        }
    }

    @Override
    public V call() throws Exception {
        return this.value;
    }

    @Override
    public boolean cancel(boolean b) {
        if (this.future.isCancelled()) {
            return false;
        }

        for (ITaskListener<V> listener : this.listeners) {
            listener.onCancelled(this);
        }
        return this.future.cancel(b);
    }

    @Override
    public boolean isCancelled() {
        return this.future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return this.future.isDone();
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return this.future.get();
    }

    @Override
    public V get(long l, @NotNull TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.future.get(l, timeUnit);
    }
}
