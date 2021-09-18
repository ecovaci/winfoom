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

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.kpax.winfoom.annotation.NotNull;
import org.springframework.util.Assert;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Various exception related utility methods.
 */
@UtilityClass
public class Throwables {

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
