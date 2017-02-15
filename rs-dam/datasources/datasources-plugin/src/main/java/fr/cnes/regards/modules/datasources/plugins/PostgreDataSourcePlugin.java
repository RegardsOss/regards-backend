/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.datasources.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.modules.datasources.plugins.interfaces.IConnectionPlugin;
import fr.cnes.regards.modules.datasources.plugins.interfaces.IDBConnectionPlugin;
import fr.cnes.regards.modules.datasources.plugins.interfaces.IDataSourcePlugin;
import fr.cnes.regards.modules.datasources.utils.AbstractDataObjectMapping;
import fr.cnes.regards.modules.datasources.utils.DataSourceAttributeMapping;
import fr.cnes.regards.modules.datasources.utils.DataSourceModelMapping;
import fr.cnes.regards.modules.datasources.utils.ModelMappingAdapter;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.models.domain.Model;

/**
 * Class DefaultESConnectionPlugin
 *
 * A default {@link Plugin} of type {@link IConnectionPlugin}.<br>
 * Allows to search in a {@link DataSource}.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@Plugin(author = "CSSI", version = "1.0-SNAPSHOT", description = "Allows data extraction to a PostgreSql database")
public class PostgreDataSourcePlugin extends AbstractDataObjectMapping implements IDataSourcePlugin {

    /**
     * The SQL request parameter name
     */
    public static final String REQUEST_PARAM = "requestSQL";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PostgreDataSourcePlugin.class);

    /**
     * The connection to the database
     */
    @PluginParameter(name = CONNECTION_PARAM)
    private IDBConnectionPlugin dbConnection;

    /**
     * The SQL request
     */
    @PluginParameter(name = REQUEST_PARAM)
    private String requestSql;

    /**
     * THe {@link Model} to used by the {@link Plugin} in JSon format.
     */
    @PluginParameter(name = MODEL_PARAM)
    private String modelJSon;

    /**
     * The mapping between the attributes in the {@link Model} and the data source
     */
    private DataSourceModelMapping dataSourceMapping;

    /**
     * Init method
     */
    @PluginInit
    private void initPlugin() {
        LOG.info("Init method call : " + this.getClass().getName() + "connection=" + this.dbConnection.toString()
                + "model=" + this.modelJSon + "requete=" + this.requestSql);

        LOG.info("Init method call : "
                + (this.dbConnection.testConnection() ? "CONNECTION_PARAM IS VALID" : "ERROR CONNECTION_PARAM"));

        // Converts the modelJson to a list of DataSourceAttributeMapping
        loadModel();
    }

    /**
     * Converts the mapping between the attribute of the datasource and the attributes of the model from a JSon
     * representation to a {@link List} of {@link DataSourceAttributeMapping}.
     */
    private void loadModel() {
        ModelMappingAdapter adapter = new ModelMappingAdapter();
        try {
            dataSourceMapping = adapter.read(new JsonReader(new StringReader(this.modelJSon)));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public int getRefreshRate() {
        // in seconds, 30 minutes
        return 1800;
    }

    @Override
    public boolean isOutOfDate() {
        return true;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Page<DataObject> findAll(String pTenant, Pageable pPageable, LocalDateTime pDate) {
        Connection conn = dbConnection.getConnection();
        if (conn == null) {
            LOG.error("Unable to obtain a database connection.");
            return null;
        }

        Page<DataObject> pages = findAllApplyPageAndDate(pTenant, conn, requestSql, pPageable, pDate);

        try {
            conn.close();
        } catch (SQLException e) {
            LOG.error(e.getMessage(), e);
        }

        return pages;
    }

    @Override
    public Page<DataObject> findAll(String pTenant, Pageable pPageable) {
        return findAll(pTenant, pPageable, null);
    }

    @Override
    protected DataSourceModelMapping getModelMapping() {
        return dataSourceMapping;
    }

}
