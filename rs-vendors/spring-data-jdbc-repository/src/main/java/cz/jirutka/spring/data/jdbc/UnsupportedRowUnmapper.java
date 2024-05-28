/*
 * Copyright 2012-2014 Tomasz Nurkiewicz <nurkiewicz@gmail.com>.
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
package cz.jirutka.spring.data.jdbc;

import java.util.Map;

/**
 * No-operational implementation of {@link RowUnmapper} that just
 * throws {@link UnsupportedOperationException}.
 */
public class UnsupportedRowUnmapper<T> implements RowUnmapper<T> {

    public Map<String, Object> mapColumns(T o) {
        throw new UnsupportedOperationException(
            "This repository is read-only, it can't store or update entities");
    }
}
