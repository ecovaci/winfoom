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

package org.kpax.winfoom.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.LifeCycle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Logback configuration class, used to add various context variables.
 */
public class LoggerStartupListener extends ContextAwareBase implements LoggerContextListener, LifeCycle {

    private boolean started = false;

    @Override
    public void start() {
        if (started) {
            return;
        }
        String configLocation = System.getenv(SystemConfig.WINFOOM_CONFIG_ENV);
        if (configLocation == null) {
            configLocation = System.getProperty("user.home");
        }
        Path logDirPath = Paths.get(configLocation, SystemConfig.APP_HOME_DIR_NAME, "logs");
        if (!Files.exists(logDirPath)) {
            try {
                Files.createDirectories(logDirPath);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        Context context = getContext();
        context.putProperty("LOG_PATH", logDirPath.toString());

        started = true;
    }

    @Override
    public void stop() {
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {

    }

    @Override
    public void onReset(LoggerContext context) {

    }

    @Override
    public void onStop(LoggerContext context) {

    }

    @Override
    public void onLevelChange(ch.qos.logback.classic.Logger logger, Level level) {

    }
}