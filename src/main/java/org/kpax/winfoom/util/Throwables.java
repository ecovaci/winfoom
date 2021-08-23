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

package org.kpax.winfoom.util;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Various exception related utility methods.
 */
public class Throwables {

    private Throwables() {
    }

    /**
     * Returns the first {@link Throwable} object in the exception chain that matches one of the provided types.
     *
     * @param throwable the throwable to inspect (not null)
     * @param types     the filtering types
     * @return the {@link Optional} wrapping the root cause
     */
    public static Optional<Throwable> getRootCause(Throwable throwable, Class<?>... types) {
        Assert.notNull(throwable, "throwable cannot be null");
        if (types == null || types.length == 0) {
            return Optional.of(ExceptionUtils.getRootCause(throwable));
        }
        List<Class<?>> typesList = Arrays.asList(types);
        return ExceptionUtils.getThrowableList(throwable).stream().
                filter(e -> typesList.stream().anyMatch(type -> type.isAssignableFrom(e.getClass()))).findAny();
    }

    /**
     * Throw the {@link Throwable} only if it is unchecked, does nothing otherwise.
     *
     * @param throwable the {@link Throwable} instance
     */
    public static void throwIfUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
    }

    /**
     * Check if a {@link Throwable} instance is an unchecked exception
     *
     * @param throwable the {@link Throwable} instance to be inspected
     * @return {@code true} iff it is an unchecked exception
     */
    public static boolean isUnchecked(Throwable throwable) {
        return throwable instanceof RuntimeException || throwable instanceof Error;
    }

    /**
     * Check if a {@link Throwable} instance is a checked exception
     *
     * @param throwable the {@link Throwable} instance to be inspected
     * @return {@code true} iff it is a checked exception
     * @see #isUnchecked(Throwable)
     */
    public static boolean isChecked(Throwable throwable) {
        return !isUnchecked(throwable);
    }

    /**
     * If the {@link Exception} class is the same as {@code cls} or a subclass of it,
     * then cast the {@link Exception} instance to {@code cls} and throw it, otherwise do nothing.
     *
     * @param e   the {@link Exception} instance
     * @param cls the {@link Class} to match against
     * @param <E> the type of the {@link Exception} to be thrown
     * @throws E
     */
    public static <E extends Exception> void throwIfMatches(
            @NotNull Throwable e,
            @NotNull Class<E> cls) throws E {
        if (cls.isAssignableFrom(e.getClass())) {
            throw cls.cast(e);
        }
    }

}
