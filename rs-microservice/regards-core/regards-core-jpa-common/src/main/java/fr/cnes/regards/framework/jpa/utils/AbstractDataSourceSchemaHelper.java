/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Common helper features
 * @author Marc Sordi
 *
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
