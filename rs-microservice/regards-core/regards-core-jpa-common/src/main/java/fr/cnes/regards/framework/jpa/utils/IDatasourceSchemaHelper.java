/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Map;

import javax.sql.DataSource;

/**
 *
 * Datasource schema migration interface
 *
 * @author Marc Sordi
 *
 */
public interface IDatasourceSchemaHelper {

    /**
    * Migrate datasource
    *
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
