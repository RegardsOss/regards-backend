/*
 * Copyright 2016 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.jirutka.spring.data.jdbc.internal;

import static org.springframework.util.ObjectUtils.toObjectArray;

public final class ObjectUtils {

    private ObjectUtils() {}

    /**
     * Wraps the given object into an object array. If the object is an object
     * array, then it returns it as-is. If it's an array of primitives, then
     * it converts it into an array of primitive wrapper objects.
     */
    public static Object[] wrapToArray(Object obj) {
        if (obj == null) {
            return new Object[0];
        }
        if (obj instanceof Object[]) {
            return (Object[]) obj;
        }
        if (obj.getClass().isArray()) {
            return toObjectArray(obj);
        }
        return new Object[]{ obj };
    }
}
