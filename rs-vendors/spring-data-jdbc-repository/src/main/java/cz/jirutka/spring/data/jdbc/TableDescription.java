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

import org.springframework.util.Assert;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;

public class TableDescription {

    private String tableName;
    private String selectClause = "*";
    private String fromClause;
    private List<String> pkColumns = singletonList("id");


    public TableDescription() {
    }

    public TableDescription(String tableName, String selectClause, String fromClause, List<String> pkColumns) {
        setTableName(tableName);
        setSelectClause(selectClause);
        setFromClause(fromClause);
        setPkColumns(pkColumns);
    }

    public TableDescription(String tableName, String selectClause, String... pkColumns) {
        this(tableName, selectClause, null, asList(pkColumns));
    }

    public TableDescription(String tableName, String idColumn) {
        this(tableName, null, idColumn);
    }


    /**
     * @see #setTableName(String)
     * @throws IllegalStateException if {@code tableName} is not set.
     */
    public String getTableName() {
        Assert.state(tableName != null, "tableName must not be null");
        return tableName;
    }

    /**
     * @param tableName The table name.
     * @throws IllegalArgumentException if {@code tableName} is blank.
     */
    public void setTableName(String tableName) {
        Assert.hasText(tableName, "tableName must not be blank");
        this.tableName = tableName;
    }

    /**
     * @see #setSelectClause(String)
     */
    public String getSelectClause() {
        return selectClause;
    }

    /**
     * @param selectClause The expression to be used in SELECT clause, i.e.
     *        list of columns to be retrieved. Default is {@code *}.
     */
    public void setSelectClause(String selectClause) {
        this.selectClause = selectClause != null ? selectClause : "*";
    }

    /**
     * @see #setSelectClause(String)
     */
    public String getFromClause() {
        return fromClause != null ? fromClause : getTableName();
    }

    /**
     * @param fromClause The expression to be used in SELECT ... FROM clause,
     *        i.e. table and join clauses. Defaults to {@link #getTableName()}.
     */
    public void setFromClause(String fromClause) {
        this.fromClause = fromClause;
    }

    /**
     * @see #setFromClause(String)
     */
    public List<String> getPkColumns() {
        return pkColumns;
    }

    /**
     * @param pkColumns A list of columns names that are part of the table's
     *        primary key.
     * @throws IllegalArgumentException if {@code pkColumn} is empty.
     */
    public void setPkColumns(List<String> pkColumns) {
        Assert.notEmpty(pkColumns, "At least one primary key column must be provided");
        this.pkColumns = unmodifiableList(pkColumns);
    }

    /**
     * @see #setPkColumns(List)
     */
    public void setPkColumns(String... idColumns) {
        setPkColumns(asList(idColumns));
    }
}
