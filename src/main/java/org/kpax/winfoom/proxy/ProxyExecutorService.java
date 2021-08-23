/*
 *  Copyright (c) 2020. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy;

import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.proxy.listener.StopListener;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A wrapper for {@link ThreadPoolExecutor} that forbids {@link #shutdown()}, {@link #shutdownNow()}
 * and {@link #awaitTermination(long, TimeUnit)}.
 */
@Slf4j
@Order(2)
@Component
public class ProxyExecutorService implements ExecutorService, StopListener {

    private final SingletonSupplier<ThreadPoolExecutor> threadPoolSupplier;

    public ProxyExecutorService() {
        this.threadPoolSupplier =
                new SingletonSupplier<>(() -> new ThreadPoolExecutor(0, Integer.MAX_VALUE,
                        60L, TimeUnit.SECONDS, new SynchronousQueue<>(),
                        new DefaultThreadFactory()));
    }

    public void execute(Runnable task) {
        this.threadPoolSupplier.get().execute(task);
    }

    public void shutdown() {
        throw new IllegalStateException("Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
    }

    public List<Runnable> shutdownNow() {
        throw new IllegalStateException("Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) {
        throw new IllegalStateException("Manual shutdown not supported - ExecutorServiceAdapter is dependent on an external lifecycle");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return threadPoolSupplier.get().submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return threadPoolSupplier.get().submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return threadPoolSupplier.get().submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return threadPoolSupplier.get().invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return threadPoolSupplier.get().invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return threadPoolSupplier.get().invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return threadPoolSupplier.get().invokeAny(tasks, timeout, unit);
    }

    public boolean isShutdown() {
        return threadPoolSupplier.hasValue() && threadPoolSupplier.get().isShutdown();
    }

    public boolean isTerminated() {
        return threadPoolSupplier.hasValue() && threadPoolSupplier.get().isTerminated();
    }

    @Override
    public void onStop() {
        logger.debug("Reset the proxy executor service");
        threadPoolSupplier.reset(ExecutorService::shutdownNow);
    }

    public static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        public DefaultThreadFactory() {
            SecurityManager securityManager = System.getSecurityManager();
            group = (securityManager != null) ? securityManager.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(group, runnable,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);

            // Make sure all threads are daemons!
            if (!thread.isDaemon()) {
                thread.setDaemon(true);
            }

            if (thread.getPriority() != Thread.NORM_PRIORITY) {
                thread.setPriority(Thread.NORM_PRIORITY);
            }
            return thread;
        }
    }
}