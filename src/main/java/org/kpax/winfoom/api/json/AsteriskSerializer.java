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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Optional;

public class AsteriskSerializer extends StdSerializer<Object> implements ContextualSerializer {

    private String asterisk;

    public AsteriskSerializer() {
        super(Object.class);
    }

    public AsteriskSerializer(String asterisk) {
        super(Object.class);
        this.asterisk = asterisk;
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializerProvider, BeanProperty property) {
        Optional<Asterisk> optionalAsterisk = Optional.ofNullable(property)
                .map(prop -> prop.getAnnotation(Asterisk.class));
        return new AsteriskSerializer(optionalAsterisk.map(Asterisk::value).orElse(null));
    }

    @Override
    public void serialize(Object obj, JsonGenerator gen, SerializerProvider prov) throws IOException {
        gen.writeString(asterisk);
    }
}