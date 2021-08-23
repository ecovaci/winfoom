/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 */

package org.kpax.winfoom.config;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Provide information about the current operating system, Spring's active profiles.
 */
public class SystemContext {

    /**
     * The name of the current operating system.
     */
    public static final String OS_NAME = System.getProperty("os.name");

    /**
     * Is Windows the current operating system?
     */
    public static final boolean IS_OS_WINDOWS = OS_NAME.toLowerCase(Locale.ROOT).startsWith("windows");

    /**
     * Is the application running in graphical mode?
     */
    public static final boolean IS_GUI_MODE = Optional.ofNullable(System.getProperty("spring.profiles.active")).
            map(s -> List.of(s.split(",")).contains("gui")).
            orElse(false);

    /**
     * Set various system properties.
     * <p>Currently, only config location is set.
     */
    public static void setEnvironment() {
        String configLocation = System.getenv(SystemConfig.WINFOOM_CONFIG_ENV);
        System.setProperty(SystemConfig.WINFOOM_CONFIG_ENV, configLocation != null ? configLocation : System.getProperty("user.home"));
        System.out.println("Config location: " + System.getProperty(SystemConfig.WINFOOM_CONFIG_ENV));
    }

}
