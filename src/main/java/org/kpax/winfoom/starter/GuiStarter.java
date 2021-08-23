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

package org.kpax.winfoom.starter;

import lombok.extern.slf4j.Slf4j;
import org.kpax.winfoom.util.SwingUtils;
import org.kpax.winfoom.view.AppFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Profile("gui")
@Component
public class GuiStarter implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private AppFrame appFrame;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        appFrame.setLocationRelativeTo(null);
        logger.info("Launch the GUI");
        try {
            appFrame.activate();
        } catch (Exception e) {
            logger.error("GUI error", e);
            SwingUtils.showErrorMessage("Failed to load the graphical interface." +
                    "<br>Please check the application's log file.");
            System.exit(1);
        }
    }
}
