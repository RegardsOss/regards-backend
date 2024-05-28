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

import static java.lang.Math.max;

public final class StringUtils {

    private StringUtils() {}

    /**
     * Repeats the given String {@code count}-times to form a new String, with
     * the {@code separator} injected between.
     *
     * @param str The string to repeat.
     * @param separator The string to inject between.
     * @param count Number of times to repeat {@code str}; negative treated
     *              as zero.
     * @return A new String.
     */
    public static String repeat(String str, String separator, int count) {
        StringBuilder sb = new StringBuilder((str.length() + separator.length()) * max(count, 0));

        for (int n = 0; n < count; n++) {
            if (n > 0) sb.append(separator);
            sb.append(str);
        }
        return sb.toString();
    }
}
