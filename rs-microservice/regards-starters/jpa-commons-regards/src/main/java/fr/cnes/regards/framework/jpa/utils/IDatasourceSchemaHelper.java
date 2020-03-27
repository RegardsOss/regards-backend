/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.utils;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Datasource schema migration interface
 * @author Marc Sordi
 */
public interface IDatasourceSchemaHelper {

    /**
     * Migrate datasource
     * @param dataSource datasource to migrate
     */
    void migrate(DataSource dataSource);

    /**
     * Set datasource before {@link IDatasourceSchemaHelper#migrate()}
     * @param dataSource datasource to migrate
     */
    void setDataSource(DataSource dataSource);

    /**
     * Migrate datasource specified with {@link IDatasourceSchemaHelper#setDataSource(DataSource)}
     */
    void migrate();

    /**
     * @return Hibernate properties
     */
    Map<String, Object> getHibernateProperties();

}
