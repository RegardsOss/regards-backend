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
package cz.jirutka.spring.data.jdbc.sql;

import cz.jirutka.spring.data.jdbc.TableDescription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

public interface SqlGenerator {

    /**
     * This method is used by {@link SqlGeneratorFactory} to select a right
     * SQL Generator.
     *
     * @param metadata The database metadata.
     * @return Whether is this generator compatible with the database described
     *         by the given {@code metadata}.
     */
    boolean isCompatible(DatabaseMetaData metadata) throws SQLException;


    String count(TableDescription table);

    String deleteAll(TableDescription table);

    String deleteById(TableDescription table);

    String deleteByIds(TableDescription table, int idsCount);

    String existsById(TableDescription table);

    String insert(TableDescription table, Map<String, Object> columns);

    String selectAll(TableDescription table);

    String selectAll(TableDescription table, Pageable page);

    String selectAll(TableDescription table, Sort sort);

    String selectById(TableDescription table);

    String selectByIds(TableDescription table, int idsCount);

    String update(TableDescription table, Map<String, Object> columns);
}
