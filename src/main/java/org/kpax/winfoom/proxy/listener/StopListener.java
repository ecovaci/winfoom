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

package org.kpax.winfoom.proxy.listener;

/**
 * For every Spring singleton bean implementing this interface the method {@link #onStop()} is called
 * when the local proxy server is stopped.
 *
 * @see org.kpax.winfoom.proxy.ProxyController#stop()
 */
@FunctionalInterface
public interface StopListener extends AutoCloseable {
    void onStop();

    @Override
    default void close() {
        onStop();
    }

}
