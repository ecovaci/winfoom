/*
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

/**
 * Proxy settings container used for the native methods. Will contain the
 * Internet Explorer proxy settings as reported by windows WinHTTP API.
 *
 * @author Markus Bernhardt, Copyright 2016
 * @author Bernd Rosstauscher, Copyright 2009
 */
package org.kpax.winfoom.util.jna;

public class IEProxyConfig {

    private final boolean autoDetect;
    private final String autoConfigUrl;
    private final String proxy;
    private final String proxyBypass;

    /*************************************************************************
     * Constructor
     *
     * @param autoDetect
     *            flag is autodetect is active or not.
     * @param autoConfigUrl
     *            the URL for a PAC script
     * @param proxy
     *            the proxy server selected
     * @param proxyBypass
     *            the proxy bypass address list.
     ************************************************************************/

    public IEProxyConfig(boolean autoDetect, String autoConfigUrl, String proxy, String proxyBypass) {
        super();
        this.autoDetect = autoDetect;
        this.autoConfigUrl = autoConfigUrl;
        this.proxy = proxy;
        this.proxyBypass = proxyBypass;
    }

    /*************************************************************************
     * @return Returns the autoDetect.
     ************************************************************************/

    public boolean isAutoDetect() {
        return this.autoDetect;
    }

    /*************************************************************************
     * @return Returns the autoConfigUrl.
     ************************************************************************/

    public String getAutoConfigUrl() {
        return this.autoConfigUrl;
    }

    /*************************************************************************
     * @return Returns the proxy.
     ************************************************************************/

    public String getProxy() {
        return this.proxy;
    }

    /*************************************************************************
     * @return Returns the proxyBypass.
     ************************************************************************/

    public String getProxyBypass() {
        return this.proxyBypass;
    }

    @Override
    public String toString() {
        return "IEProxyConfig{" +
                "autoDetect=" + autoDetect +
                ", autoConfigUrl='" + autoConfigUrl + '\'' +
                ", proxy='" + proxy + '\'' +
                ", proxyBypass='" + proxyBypass + '\'' +
                '}';
    }
}