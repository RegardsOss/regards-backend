/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common helper features
 * @author Marc Sordi
 */
public abstract class AbstractDataSourceSchemaHelper implements IDatasourceSchemaHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataSourceSchemaHelper.class);

    /**
     * Hibernate properties that may impact migration configuration
     */
    protected final Map<String, Object> hibernateProperties;

    /**
     * Target datasource
     */
    private DataSource dataSource;

    public AbstractDataSourceSchemaHelper(Map<String, Object> hibernateProperties) {
        this.hibernateProperties = hibernateProperties;
    }

    @Override
    public Map<String, Object> getHibernateProperties() {
        return hibernateProperties;
    }

    @Override
    public void setDataSource(DataSource pDataSource) {
        this.dataSource = pDataSource;
    }

    @Override
    public void migrate() {
        if (dataSource != null) {
            migrate(dataSource);
        } else {
            LOGGER.warn("No datasource found for migration. Use setDataSource to specify it before");
        }
    }
}
