/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;

import java.util.Map;

import javax.sql.DataSource;

/**
 *
 * Class IMultitenantConnectionsReader
 *
 * Interface to create a custom datasources configuration reader. All datasources returned by the method getDataSources
 * are managed by regards multitenancy jpa.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public interface IMultitenantConnectionsReader {

    public Map<String, DataSource> getDataSources();
}
