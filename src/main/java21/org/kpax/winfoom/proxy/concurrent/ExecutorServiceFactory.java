/*
 *  Copyright (c) 2024. Eugen Covaci
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.proxy.concurrent;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * A factory which creates a virtual thread executor.
 */
public class ExecutorServiceFactory implements Supplier<ExecutorService> {

    public ExecutorService get()  {
        ThreadFactory factory = Thread.ofVirtual()
                .name("virtual-thread-", 0L)
                .factory();
        return Executors.newThreadPerTaskExecutor(factory);
    }

}
