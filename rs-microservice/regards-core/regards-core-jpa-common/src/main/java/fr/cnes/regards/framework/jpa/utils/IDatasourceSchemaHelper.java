/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.util.Map;

import javax.sql.DataSource;

import fr.cnes.regards.framework.jpa.exception.JpaException;

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
    * @throws JpaException if error occurs
    */
    void migrate(DataSource dataSource) throws JpaException;

    /**
     * @return Hibernate properties
     */
    Map<String, Object> getHibernateProperties();

}
