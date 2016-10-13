/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

/**
 * @author msordi
 *
 */
public class DataSourceHelper {

    /**
     * Hibernate dialect for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQLDB_HIBERNATE_DIALECT = "org.hibernate.dialect.HSQLDialect";

    /**
     * Hibernate driver class for embedded HSQL Database
     */
    public static final String EMBEDDED_HSQL_DRIVER_CLASS = "org.hsqldb.jdbcDriver";

    /**
     * Url prefix for embedded HSQL Database. Persistence into file.
     */
    public static final String EMBEDDED_HSQL_URL = "jdbc:hsqldb:file:";

    /**
     * Data source URL separator
     */
    public static final String EMBEDDED_URL_SEPARATOR = "/";

    /**
     * HSQL Embedded Data source base name. Property shutdown allow to close the embedded database when the last
     * connection is close.
     */
    public static final String EMBEDDED_URL_BASE_NAME = "applicationdb;shutdown=true;";

}
