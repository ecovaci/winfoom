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

package org.kpax.winfoom.api.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.util.HttpUtils;

/**
 * The settings DTO for API server.
 */
@Getter
@Setter
@ToString
public class SettingsDto {

    private Integer apiPort;
    private Boolean autodetect;
    private Boolean autostart;

    public void validate() throws InvalidProxySettingsException {
        if (apiPort != null) {
            if (!HttpUtils.isValidPort(apiPort)) {
                throw new InvalidProxySettingsException("Invalid apiPort, allowed range: 1 - 65535");
            }
        }
    }

}
