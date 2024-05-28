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

import java.util.ArrayList;
import java.util.List;

public final class IterableUtils {

    private IterableUtils() {}

    /**
     * Converts the given Iterable into an ArrayList.
     */
    public static <T> List<T> toList(Iterable<T> iterable) {

        if (iterable instanceof List) {
            return (List<T>) iterable;
        }

        List<T> result = new ArrayList<>();
        for (T item : iterable) {
            result.add(item);
        }

        return result;
    }
}
