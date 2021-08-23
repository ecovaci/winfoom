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

package org.kpax.winfoom.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

@Slf4j
public class BeanUtils {

    public static void copyProperties(Iterator<String> fieldNamesItr, Object source, Object destination)
            throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Map<String, Object> objectMap = PropertyUtils.describe(source);
        for (; fieldNamesItr.hasNext(); ) {
            String fieldName = fieldNamesItr.next();
            if (objectMap.containsKey(fieldName)) {
                Object fieldValue = objectMap.get(fieldName);
                logger.debug("Set property: {}={}", fieldName, fieldValue);
                PropertyUtils.setProperty(destination, fieldName, fieldValue);
            } else {
                throw new IllegalArgumentException("The source object does not contain the field [" + fieldName + "] ");
            }
        }
    }

}
