/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util.functional;

import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.util.InputOutputs;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.function.Consumer;


/**
 * A {@link TripleExceptionSupplier} decorator that caches a singleton value and
 * makes it available from {@link #get()}.
 * <p>The provided {@link TripleExceptionSupplier} must not return {@code null}.
 * <p>It is also possible to refresh the stored value (see {@link #reset()} method).
 * <p>Note: Refreshing might break the null safety of {@link #get()} method in a multi-threaded environment.
 *
 * @param <T>  the type of value supplied by this supplier
 * @param <E1> the first {@link Exception} type
 * @param <E2> the second {@link Exception} type
 * @param <E3> the third {@link Exception} type
 */
public class TripleExceptionSingletonSupplier<T, E1 extends Exception, E2 extends Exception, E3 extends Exception>
        implements TripleExceptionSupplier<T, E1, E2, E3> {

    /**
     * For thread safety purposes.
     */
    private final Object LOCK = new Object();

    /**
     * The {@link java.util.function.Supplier} used to initialize the value.
     */
    private final TripleExceptionSupplier<T, E1, E2, E3> supplier;

    /**
     * The cached value.
     */
    private volatile T t;

    /**
     * Constructor.
     *
     * @param supplier the not null supplier
     */
    public TripleExceptionSingletonSupplier(TripleExceptionSupplier<T, E1, E2, E3> supplier) {
        Assert.notNull(supplier, "supplier cannot be null");
        this.supplier = supplier;
    }

    /**
     * Get the cached value (if any) otherwise a new value is created in a thread safe manner.
     *
     * @return the value, never null.
     */
    @NotNull
    @Override
    public T get() throws E1, E2, E3 {
        if (t == null) {
            synchronized (LOCK) {
                if (t == null) {
                    t = supplier.get();
                    Assert.state(t != null, "No instance from supplier");
                }
            }
        }
        return t;
    }

    public Optional<T> value() {
        return Optional.ofNullable(t);
    }

    /**
     * If the value is an {@link AutoCloseable} close it, then nullify the value in a thread safe manner.
     */
    public void reset() {
        synchronized (LOCK) {
            if (t instanceof AutoCloseable) {
                InputOutputs.close((AutoCloseable) t);
            }
            t = null;
        }
    }

    /**
     * Consume a {@link Consumer} before resetting.
     *
     * @param beforeReset the {@link Consumer} to call before resetting (not null).
     */
    public void reset(@NotNull Consumer<T> beforeReset) {
        if (t != null) {
            beforeReset.accept(t);
        }
        reset();
    }

    /**
     * Check whether the value is not null.
     *
     * @return {@code true} iff the value is not null
     */
    public boolean hasValue() {
        return t != null;
    }

}
