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

package org.kpax.winfoom.util;

import lombok.Getter;

/**
 * An immutable class that encapsulates a GUI message.
 *
 * @author Eugen Covaci {@literal eugen.covaci.q@gmail.com}
 * Created on 2/28/2020
 */
@Getter
public final class Message {

    /**
     * The message type.
     */
    private final MessageType type;

    /**
     * The message text.
     */
    private final String text;

    private Message(MessageType type, String text) {
        this.type = type;
        this.text = text;
    }

    public static Message of(MessageType type, String text) {
        return new Message(type, text);
    }

    public static Message error(String text) {
        return new Message(MessageType.ERROR, text);
    }

    public static Message warning(String text) {
        return new Message(MessageType.WARNING, text);
    }

    public static Message info(String text) {
        return new Message(MessageType.INFO, text);
    }

    public enum MessageType {
        ERROR("Error"), INFO("Information"), WARNING("Warning");

        private final String label;

        MessageType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
