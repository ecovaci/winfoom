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

package org.kpax.winfoom;

import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.config.SystemConfig;
import org.kpax.winfoom.util.Base64DecoderPropertyEditor;
import org.kpax.winfoom.util.SwingUtils;
import org.springframework.beans.factory.config.CustomEditorConfigurer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.swing.*;
import java.beans.PropertyEditor;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * The entry point for Winfoom application.
 */
@Slf4j
@EnableScheduling
@SpringBootApplication
public class FoomApplication {

    @Bean
    static CustomEditorConfigurer propertyEditorRegistrar() {
        CustomEditorConfigurer customEditorConfigurer = new CustomEditorConfigurer();
        Map<Class<?>, Class<? extends PropertyEditor>> customEditors = new HashMap<>();
        customEditors.put(String.class, Base64DecoderPropertyEditor.class);
        customEditorConfigurer.setCustomEditors(customEditors);
        return customEditorConfigurer;
    }

    public static void main(String[] args) {
        if (SystemConfig.IS_GUI_MODE && !SystemConfig.IS_OS_WINDOWS) {
            log.error("Graphical mode is not supported on " + SystemConfig.OS_NAME + ", exit the application");
            System.exit(1);
        }

        log.info("Application started at: {}", new Date());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> log.info("Application shutdown at: {}", new Date())));

        if (SystemConfig.IS_GUI_MODE) {
            try {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            } catch (Exception e) {
                log.warn("Failed to set Windows L&F, use the default look and feel", e);
            }
        }

        log.info("Bootstrap Spring's application context");
        try {
            SpringApplication.run(FoomApplication.class, args);
        } catch (Exception e) {
            log.error("Error on bootstrapping Spring's application context", e);
            if (SystemConfig.IS_GUI_MODE) {
                SwingUtils.showErrorMessage("Failed to launch the application." +
                        "<br>Please check the application's log file.");
            }
            System.exit(1);
        }
    }


}
