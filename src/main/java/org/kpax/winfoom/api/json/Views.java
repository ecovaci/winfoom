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

package org.kpax.winfoom.api.json;

import org.kpax.winfoom.annotation.NotNull;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.config.SystemConfig;

/**
 * Jackson's views to control which fields should be JSON available, based on the operating system and proxy type.
 */
public class Views {

    public interface Windows {
    }

    public interface NonWindows {
    }

    public interface Settings {
    }

    public interface WindowsSettings extends Settings, Windows {
    }

    public interface NonWindowsSettings extends Settings, NonWindows {
    }

    public interface Common {
    }

    public interface Direct extends Common {
    }

    public interface DirectWindows extends Direct, Windows {
    }

    public interface DirectNonWindows extends Direct, NonWindows {
    }

    public interface Http extends Common {
    }

    public interface HttpWindows extends Http, Windows {
    }

    public interface HttpWindowsManual extends HttpWindows {
    }

    public interface HttpNonWindows extends Http, NonWindows {
    }

    public interface Socks4 extends Common {
    }

    public interface Socks4Windows extends Socks4, Windows {
    }

    public interface Socks4NonWindows extends Socks4, NonWindows {
    }

    public interface Socks5 extends Socks4 {
    }

    public interface Socks5Windows extends Socks5, Windows {
    }

    public interface Socks5NonWindows extends Socks5, NonWindows {
    }

    public interface Pac extends Common {
    }

    public interface PacWindows extends Pac, Windows {
    }

    public interface PacNonWindows extends Pac, NonWindows {
    }

    public static Class<?> getView(@NotNull ProxyConfig proxyConfig) {
        switch (proxyConfig.getProxyType()) {
            case DIRECT:
                if (SystemConfig.IS_OS_WINDOWS) {
                    return DirectWindows.class;
                } else {
                    return DirectNonWindows.class;
                }
            case HTTP:
                if (proxyConfig.isHttpAuthAutoMode()) {
                    return HttpWindows.class;
                } else {
                    if (SystemConfig.IS_OS_WINDOWS) {
                        return HttpWindowsManual.class;
                    }
                    return HttpNonWindows.class;
                }
            case SOCKS4:
                if (SystemConfig.IS_OS_WINDOWS) {
                    return Socks4Windows.class;
                } else {
                    return Socks4NonWindows.class;
                }
            case SOCKS5:
                if (SystemConfig.IS_OS_WINDOWS) {
                    return Socks5Windows.class;
                } else {
                    return Socks5NonWindows.class;
                }
            case PAC:
                if (SystemConfig.IS_OS_WINDOWS) {
                    return PacWindows.class;
                } else {
                    return PacNonWindows.class;
                }
            default:
                throw new IllegalArgumentException("No view for type: " + proxyConfig.getProxyType());
        }
    }

    public static Class<?> getSettingsView() {
        if (SystemConfig.IS_OS_WINDOWS) {
            return WindowsSettings.class;
        } else {
            return NonWindowsSettings.class;
        }
    }

}
