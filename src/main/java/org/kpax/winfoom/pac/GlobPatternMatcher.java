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

package org.kpax.winfoom.pac;

import lombok.extern.slf4j.Slf4j;
import org.cache2k.Cache;
import org.cache2k.Cache2kBuilder;
import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.functional.SingletonSupplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.regex.Pattern;

/**
 * GLOB pattern matcher.
 */
@Slf4j
@Component
public class GlobPatternMatcher {

    @Autowired
    private SystemConfig systemConfig;

    private final SingletonSupplier<Cache<String, Pattern>> globPatternCacheSupplier =
            new SingletonSupplier<>(() ->
                    Cache2kBuilder.of(String.class, Pattern.class)
                            .name("precompiledGlobPattern")
                            .eternal(true)
                            .entryCapacity(systemConfig.getCacheGlobPatternCapacity())
                            .build()
            );

    /**
     * Translate a GLOB pattern into a {@link Pattern} instance.
     * <p>
     * <b>Note:</b> The result is cached.
     *
     * @param glob the GLOB pattern.
     * @return the {@link Pattern} instance.
     * @see #convertGlobToRegEx(String)
     */
    public Pattern toPattern(@NotNull String glob) {
        Assert.notNull(glob, "glob cannot be null");
        Pattern pattern = globPatternCacheSupplier.get().get(glob);
        if (pattern == null) {
            logger.debug("Create pattern for {}", glob);
            String regexPattern = convertGlobToRegEx(glob.trim());
            logger.debug("glob regexPattern={}", regexPattern);
            pattern = Pattern.compile(regexPattern);
            globPatternCacheSupplier.get().put(glob, pattern);
        }
        return pattern;
    }

    /**
     * Create a regex out of a GLOB expression.
     * <ul>
     *  <li> The {@code *} character matches zero or more characters.</li>
     *  <li> {@code ?} matches exactly one character.</li>
     *  <li> {@code (ab|cd|ef)}matches one of the "parts" provided.
     *  <li>  The use of {@code [abc]} matches a range of characters.
     *        If the first character of the range is {@code !} or {@code ^} then it matches any character not in the range.</li>
     *   </ul>
     *
     * @param globExpression the GLOB expression.
     * @return the regex
     */
    public static String convertGlobToRegEx(@NotNull String globExpression) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("^");
        for (int i = 0; i < globExpression.length(); ++i) {
            char c = globExpression.charAt(i);
            switch (c) {
                case '*':
                    stringBuilder.append(".*?");
                    break;
                case '?':
                    stringBuilder.append(".{1}");
                    break;
                case '.':
                    stringBuilder.append("\\.");
                    break;
                case '\\':
                    stringBuilder.append("\\\\");
                    break;
                case '!':
                    if (i > 0 && globExpression.charAt(i - 1) == '[') {
                        stringBuilder.append('^');
                    } else {
                        stringBuilder.append(c);
                    }
                    break;
                default:
                    stringBuilder.append(c);
            }
        }
        stringBuilder.append("$");
        return stringBuilder.toString();
    }

}
