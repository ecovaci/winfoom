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
package org.kpax.winfoom.util.jna;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WTypes;

import java.util.Arrays;
import java.util.List;

/**
 * The WINHTTP_CURRENT_USER_IE_PROXY_CONFIG structure contains the Internet
 * Explorer proxy configuration information.
 *
 * @author Markus Bernhardt, Copyright 2016
 */
public class WinHttpCurrentUserIEProxyConfig extends Structure {

    /**
     * If TRUE, indicates that the Internet Explorer proxy configuration for the
     * current user specifies "automatically detect settings".
     */
    public boolean fAutoDetect;

    /**
     * Pointer to a null-terminated Unicode string that contains the
     * auto-configuration URL if the Internet Explorer proxy configuration for
     * the current user specifies "Use automatic proxy configuration".
     */
    public WTypes.LPWSTR lpszAutoConfigUrl;

    /**
     * Pointer to a null-terminated Unicode string that contains the proxy URL
     * if the Internet Explorer proxy configuration for the current user
     * specifies "use a proxy server".
     */
    public WTypes.LPWSTR lpszProxy;

    /**
     * Pointer to a null-terminated Unicode string that contains the optional
     * proxy by-pass server list.
     */
    public WTypes.LPWSTR lpszProxyBypass;

    /**
     * Create WinHttpCurrentUserIeProxyConfig structure.
     */
    public WinHttpCurrentUserIEProxyConfig() {
        super();
    }

    /**
     * Create WinHttpCurrentUserIeProxyConfig structure cast onto pre-allocated
     * memory.
     *
     * @param pointer pointer to pre-allocated memory
     */
    public WinHttpCurrentUserIEProxyConfig(Pointer pointer) {
        super(pointer);
        read();
    }

    /**
     * Return this Structure's field names in their proper order. For example,
     *
     * <pre>
     * <code>
     * protected List getFieldOrder() {
     *     return Arrays.asList(new String[] { ... });
     * }
     * </code>
     * </pre>
     *
     * <strong>IMPORTANT</strong> When deriving from an existing Structure
     * subclass, ensure that you augment the list provided by the superclass,
     * e.g.
     *
     * <pre>
     * <code>
     * protected List getFieldOrder() {
     *     List fields = new ArrayList(super.getFieldOrder());
     *     fields.addAll(Arrays.asList(new String[] { ... }));
     *     return fields;
     * }
     * </code>
     * </pre>
     * <p>
     * Field order must be explicitly indicated, since the field order as
     * returned by {@link Class#getFields()} is not guaranteed to be
     * predictable.
     *
     * @return ordered list of field names
     */
    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("fAutoDetect", "lpszAutoConfigUrl", "lpszProxy", "lpszProxyBypass");
    }

    /**
     * Tagging interface to indicate the address of an instance of the Structure
     * type is to be used within a <code>Structure</code> definition rather than
     * nesting the full Structure contents. The default behavior is to inline
     * <code>Structure</code> fields.
     */
    public static class ByReference extends WinHttpProxyInfo implements Structure.ByReference {
    }

    /**
     * Tagging interface to indicate the value of an instance of the
     * <code>Structure</code> type is to be used in function invocations rather
     * than its address. The default behavior is to treat <code>Structure</code>
     * function parameters and return values as by reference, meaning the
     * address of the structure is used.
     */
    public static class ByValue extends WinHttpProxyInfo implements Structure.ByValue {
    }

}