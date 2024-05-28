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
package cz.jirutka.spring.data.jdbc;

import org.springframework.dao.IncorrectUpdateSemanticsDataAccessException;

import static java.lang.String.format;
import static org.springframework.util.StringUtils.arrayToCommaDelimitedString;

/**
 * Exception thrown when trying to update a record that doesn't exist.
 */
public class NoRecordUpdatedException extends IncorrectUpdateSemanticsDataAccessException {

    private final String tableName;
    private final Object[] id;


    public NoRecordUpdatedException(String tableName, Object... id) {
        super(format("No record with id = {%s} exists in table %s",
                arrayToCommaDelimitedString(id), tableName));
        this.tableName = tableName;
        this.id = id.clone();
    }

    public NoRecordUpdatedException(String tableName, String msg) {
        super(msg);
        this.tableName = tableName;
        this.id = new Object[0];
    }

    public NoRecordUpdatedException(String tableName, String msg, Throwable cause) {
        super(msg, cause);
        this.tableName = tableName;
        this.id = new Object[0];
    }


    @Override
    public boolean wasDataUpdated() {
        return false;
    }

    public String getTableName() {
        return tableName;
    }

    public Object[] getId() {
        return id.clone();
    }
}
