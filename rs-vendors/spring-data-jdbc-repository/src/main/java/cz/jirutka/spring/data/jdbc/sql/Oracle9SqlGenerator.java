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
package cz.jirutka.spring.data.jdbc.sql;

import cz.jirutka.spring.data.jdbc.TableDescription;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static java.lang.String.format;

/**
 * SQL Generator for Oracle up to 11g. If you have 12g or newer, then use
 * {@link SQL2008SqlGenerator}.
 *
 * @see <a href="https://explainextended.com/2009/05/06/oracle-row_number-vs-rownum/">
 *      Oracle: ROW_NUMBER vs ROWNUM</a>
 */
public class Oracle9SqlGenerator extends DefaultSqlGenerator {

    @Override
    public boolean isCompatible(DatabaseMetaData metadata) throws SQLException {
        return "Oracle".equals(metadata.getDatabaseProductName());
    }

    @Override
    public String selectAll(TableDescription table, Pageable page) {
        Sort sort = page.getSort() != null ? page.getSort() : sortById(table);

        return format("SELECT t2__.* FROM ( "
                + "SELECT t1__.*, ROWNUM as rn__ FROM ( %s ) t1__ "
                + ") t2__ WHERE t2__.rn__ > %d AND ROWNUM <= %d",
            selectAll(table, sort), page.getOffset(), page.getPageSize());
    }
}
