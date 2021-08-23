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

import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WTypes;
import org.kpax.winfoom.util.InputOutputs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pointer to a LPWSTR.
 *
 * <p>
 * LPWSTR is itself a pointer, so a pointer to an LPWSTR is really a
 * pointer-to-pointer. This class hides this complexity and also takes care
 * of memory disposal.
 *
 * <p>
 * The class is useful where the Windows function <i>returns</i> a result
 * into a variable of type {@code LPWSTR*}. The class currently has no
 * setters so it isn't useful for the opposite case, i.e. where a Windows
 * function <i>accepts</i> a {@code LPWSTR*} as its input.
 *
 * @author phansson
 */
public class LPWSTRByReference extends PointerType implements AutoCloseable {

    private final Logger logger = LoggerFactory.getLogger(LPWSTRByReference.class);

    public LPWSTRByReference() {
        setPointer(new CloseableMemory(Pointer.SIZE));
        // memory cleanup
        getPointer().setPointer(0, null);
    }

    /**
     * Gets the LPWSTR from this pointer. In general its a lot more
     * convenient simply to use {@link #getString() getString}.
     *
     * @return LPWSTR from this pointer
     */
    public WTypes.LPWSTR getValue() {
        Pointer pointer = getPointerToString();
        return pointer != null ? new WTypes.LPWSTR(pointer) : null;
    }

    /**
     * Gets the string as pointed to by the LPWSTR or {@code null} if
     * there's no LPWSTR.
     *
     * @return LPWSTR from this pointer
     */
    public String getString() {
        return getValue() != null ? getValue().getValue() : null;
    }

    private Pointer getPointerToString() {
        return getPointer().getPointer(0);
    }

    /**
     * Memory disposal.
     */
    @Override
    public void close() {
        try {
            // Free the memory occupied by the string returned
            // from the Win32 function.
            Pointer strPointer = getPointerToString();
            if (strPointer != null) {
                Pointer result = Kernel32.INSTANCE.GlobalFree(strPointer);
                if (result != null) {
                    // The call to GlobalFree has failed. This should never
                    // happen. If it really does happen, there isn't much we
                    // can do about it other than logging it.
                    logger.warn(
                            "Windows function GlobalFree failed while freeing memory for {} object",
                            getClass());
                }
            }
        } catch (Exception e) {
            logger.warn("Fail to free the memory occupied by the string returned from the Win32 function", e);
        } finally {
            // This will free the memory of the pointer-to-pointer
            InputOutputs.close((CloseableMemory) getPointer());
        }
    }

}
