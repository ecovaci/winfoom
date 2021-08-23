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

package org.kpax.winfoom.exception;

/**
 * Signal an invalid Proxy Auto Config file (wrong syntax, {@code findProxyForURL} function is invalid or
 * non-existent etc.)
 */
public class PacFileException extends Exception {
    public PacFileException() {
    }

    public PacFileException(String message) {
        super(message);
    }

    public PacFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public PacFileException(Throwable cause) {
        super(cause);
    }
}
