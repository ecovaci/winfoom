/**
 * Copyright (c)  2016 Markus Bernhardt
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Copyright (c) 2009 Bernd Rosstauscher
 * <p>
 * All rights reserved.
 * <p>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the <organization> nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 * <p>
 * THIS SOFTWARE IS PROVIDED BY BERND ROSSTAUSCHER ''AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL BERND ROSSTAUSCHER BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/*
 * Modifications copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.util.jna;

import com.sun.jna.LastErrorException;
import com.sun.jna.platform.win32.WinDef;
import org.apache.commons.lang3.StringUtils;
import org.kpax.winfoom.annotation.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static helper methods for Windows {@code WinHttp} functions.
 *
 * @author phansson
 */
public class WinHttpHelpers {

    private static final Logger logger = LoggerFactory.getLogger(WinHttpHelpers.class);

    private WinHttpHelpers() {
    }

    /**
     * Finds the URL for the Proxy Auto-Configuration (PAC) file using WPAD.
     * This is merely a wrapper around
     * {@link WinHttp#WinHttpDetectAutoProxyConfigUrl(WinDef.DWORD, LPWSTRByReference)}
     * WinHttpDetectAutoProxyConfigUrl}
     *
     * <p>
     * This method is blocking and may take some time to execute.
     *
     * @param dwAutoDetectFlags flags for auto detection
     * @return the url of the PAC file or {@code null} if it cannot be located
     * using WPAD method.
     */
    public static String detectAutoProxyConfigUrl(WinDef.DWORD dwAutoDetectFlags) {
        try (LPWSTRByReference ppwszAutoConfigUrl = new LPWSTRByReference()) {
            boolean result = WinHttp.INSTANCE.WinHttpDetectAutoProxyConfigUrl(dwAutoDetectFlags, ppwszAutoConfigUrl);
            if (result) {
                return ppwszAutoConfigUrl.getString();
            }
        } catch (LastErrorException ex) {
            if (ex.getErrorCode() == WinHttp.ERROR_WINHTTP_AUTODETECTION_FAILED) {
                // This error is to be expected. It just means that the lookup
                // using either DHCP, DNS or both, failed because there wasn't
                // a useful reply from DHCP / DNS. (meaning the site hasn't
                // configured their DHCP Server or their DNS Server for WPAD)
                logger.debug("The DHCP Server or the DNS Server is not configured for WPAD");
            } else {
                // Something more serious is wrong. There isn't much we can do
                // about it but at least we would like to log it.
                logger.warn("Windows function WinHttpDetectAutoProxyConfigUrl returned error", ex);
            }
        }
        return null;
    }

    public static IEProxyConfig readIEProxyConfig() {
        // Retrieve the IE proxy configuration.
        WinHttpCurrentUserIEProxyConfig winHttpCurrentUserIeProxyConfig = new WinHttpCurrentUserIEProxyConfig();
        boolean result = WinHttp.INSTANCE.WinHttpGetIEProxyConfigForCurrentUser(winHttpCurrentUserIeProxyConfig);
        if (result) {
            // Create IEProxyConfig instance
            return new IEProxyConfig(winHttpCurrentUserIeProxyConfig.fAutoDetect,
                    winHttpCurrentUserIeProxyConfig.lpszAutoConfigUrl != null
                            ? winHttpCurrentUserIeProxyConfig.lpszAutoConfigUrl.getValue() : null,
                    winHttpCurrentUserIeProxyConfig.lpszProxy != null ? winHttpCurrentUserIeProxyConfig.lpszProxy.getValue()
                            : null,
                    winHttpCurrentUserIeProxyConfig.lpszProxyBypass != null
                            ? winHttpCurrentUserIeProxyConfig.lpszProxyBypass.getValue() : null);
        }
        return null;
    }


    public static String findPacFileLocation(@NotNull final IEProxyConfig ieSettings) {
        String pacUrl = null;
        if (ieSettings.isAutoDetect()) {
            logger.debug("Auto detecting script URL ...");
            // This will take some time.
            WinDef.DWORD dwAutoDetectFlags = new WinDef.DWORD(
                    WinHttp.WINHTTP_AUTO_DETECT_TYPE_DHCP | WinHttp.WINHTTP_AUTO_DETECT_TYPE_DNS_A);
            pacUrl = WinHttpHelpers.detectAutoProxyConfigUrl(dwAutoDetectFlags);
            logger.debug("Detected script URL: {}", pacUrl);
        }
        if (pacUrl == null) {
            pacUrl = ieSettings.getAutoConfigUrl();
        }
        logger.debug("IE uses script: {}", pacUrl);
        if (StringUtils.isNotEmpty(pacUrl)) {
            // Fix for issue 9
            // If the IE has a file URL and it only starts has 2 slashes,
            // add a third so it can be properly converted to the URL class
            if (pacUrl.startsWith("file://") && !pacUrl.startsWith("file:///")) {
                pacUrl = "file:///" + pacUrl.substring(7);
            }
        }
        return pacUrl;
    }
}
