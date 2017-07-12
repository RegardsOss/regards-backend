/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.datasources.plugins.datasource;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParametersFactory;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.datasources.domain.AbstractAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.DataSourceModelMapping;
import fr.cnes.regards.modules.datasources.domain.DynamicAttributeMapping;
import fr.cnes.regards.modules.datasources.domain.ModelMappingAdapter;
import fr.cnes.regards.modules.datasources.domain.StaticAttributeMapping;
import fr.cnes.regards.modules.datasources.plugins.DefaultOracleConnectionPlugin;
import fr.cnes.regards.modules.datasources.plugins.OracleDataSourceFromSingleTablePlugin;
import fr.cnes.regards.modules.datasources.plugins.PostgreDataSourceFromSingleTablePlugin;
import fr.cnes.regards.modules.datasources.plugins.exception.DataSourceException;
import fr.cnes.regards.modules.datasources.plugins.interfaces.IDataSourceFromSingleTablePlugin;
import fr.cnes.regards.modules.datasources.utils.exceptions.DataSourcesPluginException;
import fr.cnes.regards.modules.entities.domain.DataObject;
import fr.cnes.regards.modules.models.domain.attributes.AttributeType;
import fr.cnes.regards.plugins.utils.PluginUtils;

/**
 * @author Christophe Mertz
 */
@RunWith(SpringRunner.class)
@TestPropertySource(locations = { "classpath:datasource-test.properties" })
@ComponentScan(basePackages = { "fr.cnes.regards.modules.datasources.utils" })
@Ignore
public class OracleDataSourceFromSingleTablePluginWithoutLastUpdateTest {

    private static final Logger LOG = LoggerFactory
            .getLogger(OracleDataSourceFromSingleTablePluginWithoutLastUpdateTest.class);

    private static final String PLUGIN_CURRENT_PACKAGE = "fr.cnes.regards.modules.datasources.plugins";

    private static final String TABLE_NAME_TEST = "T_DATA_OBJECTS";

    private static final String TENANT = "ORA_TENANT";

    @Value("${oracle.datasource.host}")
    private String dbHost;

    @Value("${oracle.datasource.port}")
    private String dbPort;

    @Value("${oracle.datasource.name}")
    private String dbName;

    @Value("${oracle.datasource.username}")
    private String dbUser;

    @Value("${oracle.datasource.password}")
    private String dbPassword;

    private IDataSourceFromSingleTablePlugin plgDBDataSource;

    private DataSourceModelMapping dataSourceModelMapping;

    private final ModelMappingAdapter adapter = new ModelMappingAdapter();

    private Map<Long, Object> pluginCacheMap = new HashMap<>();

    /**
     * Initialize the plugin's parameter
     *
     * @throws DataSourcesPluginException
     * @throws SQLException
     */
    @Before
    public void setUp() throws DataSourcesPluginException, SQLException {

        /*
         * Initialize the AbstractAttributeMapping
         */
        buildModelAttributes();

        /*
         * Instantiate the SQL DataSource plugin
         */
        List<PluginParameter> parameters;

        parameters = PluginParametersFactory.build()
                .addParameterPluginConfiguration(OracleDataSourceFromSingleTablePlugin.CONNECTION_PARAM,
                                                 getOracleConnectionConfiguration())
                .addParameter(PostgreDataSourceFromSingleTablePlugin.REFRESH_RATE, "1800")
                .addParameter(OracleDataSourceFromSingleTablePlugin.TABLE_PARAM, TABLE_NAME_TEST)
                .addParameter(OracleDataSourceFromSingleTablePlugin.MODEL_PARAM, adapter.toJson(dataSourceModelMapping))
                .getParameters();

        plgDBDataSource = PluginUtils.getPlugin(parameters, OracleDataSourceFromSingleTablePlugin.class,
                                                Arrays.asList(PLUGIN_CURRENT_PACKAGE), pluginCacheMap);

        // Do not launch tests is Database is not available
        Assume.assumeTrue(plgDBDataSource.getDBConnection().testConnection());
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SRC_100")
    @Requirement("REGARDS_DSL_DAM_SRC_110")
    @Requirement("REGARDS_DSL_DAM_SRC_140")
    @Purpose("The system allows to create a plugin to get a subset of the datasource's data")
    public void getDataSourceIntrospection() throws DataSourceException {
        OffsetDateTime lastUpdateDate = OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).minusYears(6);
        Page<DataObject> ll = plgDBDataSource.findAll(TENANT, new PageRequest(0, 1000), lastUpdateDate);
        Assert.assertNotNull(ll);
        Assert.assertEquals(1000, ll.getContent().size());

        ll.getContent().forEach(d -> Assert.assertNotNull(d.getIpId()));
        ll.getContent().forEach(d -> Assert.assertNotNull(d.getSipId()));
        ll.getContent().forEach(d -> Assert.assertTrue(0 < d.getProperties().size()));

        ll = plgDBDataSource.findAll(TENANT, new PageRequest(1, 1000));
        Assert.assertNotNull(ll);
        Assert.assertEquals(1000, ll.getContent().size());

        ll.getContent().forEach(d -> Assert.assertNotNull(d.getIpId()));
        ll.getContent().forEach(d -> Assert.assertNotNull(d.getSipId()));
        ll.getContent().forEach(d -> Assert.assertTrue(0 < d.getProperties().size()));
    }

    /**
     * Define the {@link PluginConfiguration} for a {@link DefaultOracleConnectionPlugin} to connect to the Oracle
     * database.
     *
     * @return the {@link PluginConfiguration} @
     */
    private PluginConfiguration getOracleConnectionConfiguration() {
        final List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultOracleConnectionPlugin.USER_PARAM, dbUser)
                .addParameter(DefaultOracleConnectionPlugin.PASSWORD_PARAM, dbPassword)
                .addParameter(DefaultOracleConnectionPlugin.DB_HOST_PARAM, dbHost)
                .addParameter(DefaultOracleConnectionPlugin.DB_PORT_PARAM, dbPort)
                .addParameter(DefaultOracleConnectionPlugin.DB_NAME_PARAM, dbName)
                .addParameter(DefaultOracleConnectionPlugin.MAX_POOLSIZE_PARAM, "3")
                .addParameter(DefaultOracleConnectionPlugin.MIN_POOLSIZE_PARAM, "1").getParameters();

        PluginConfiguration plgConf = PluginUtils.getPluginConfiguration(parameters,
                                                                         DefaultOracleConnectionPlugin.class,
                                                                         Arrays.asList(PLUGIN_CURRENT_PACKAGE));
        pluginCacheMap.put(plgConf.getId(),
                           PluginUtils.getPlugin(plgConf, plgConf.getPluginClassName(),
                                                 Arrays.asList(PLUGIN_CURRENT_PACKAGE), pluginCacheMap));
        return plgConf;
    }

    private void buildModelAttributes() {
        List<AbstractAttributeMapping> attributes = new ArrayList<AbstractAttributeMapping>();

        attributes.add(new StaticAttributeMapping(AbstractAttributeMapping.PRIMARY_KEY, AttributeType.INTEGER,
                "DATA_OBJECT_ID"));

        attributes.add(new DynamicAttributeMapping("FILE_SIZE", AttributeType.INTEGER, "FILE_SIZE"));
        attributes.add(new DynamicAttributeMapping("FILE_TYPE", AttributeType.STRING, "FILE_TYPE"));
        attributes.add(new DynamicAttributeMapping("FILE_NAME_ORIGINE", AttributeType.STRING, "FILE_NAME_ORIGINE"));

        attributes.add(new DynamicAttributeMapping("DATA_SET_ID", AttributeType.INTEGER, "DATA_SET_ID"));
        attributes.add(new DynamicAttributeMapping("DATA_TITLE", AttributeType.STRING, "DATA_TITLE"));
        attributes.add(new DynamicAttributeMapping("DATA_AUTHOR", AttributeType.STRING, "DATA_AUTHOR"));
        attributes.add(new DynamicAttributeMapping("DATA_AUTHOR_COMPANY", AttributeType.STRING, "DATA_AUTHOR_COMPANY"));

        attributes.add(new DynamicAttributeMapping("STOP_DATE", AttributeType.DATE_ISO8601, "STOP_DATE"));
        attributes.add(new DynamicAttributeMapping("DATA_CREATION_DATE", AttributeType.DATE_ISO8601,
                "DATA_CREATION_DATE"));

        attributes.add(new DynamicAttributeMapping("MIN_LONGITUDE", AttributeType.INTEGER, "MIN_LONGITUDE"));
        attributes.add(new DynamicAttributeMapping("MAX_LONGITUDE", AttributeType.INTEGER, "MAX_LONGITUDE"));
        attributes.add(new DynamicAttributeMapping("MIN_LATITUDE", AttributeType.INTEGER, "MIN_LATITUDE"));
        attributes.add(new DynamicAttributeMapping("MAX_LATITUDE", AttributeType.INTEGER, "MAX_LATITUDE"));
        attributes.add(new DynamicAttributeMapping("MIN_ALTITUDE", AttributeType.INTEGER, "MIN_ALTITUDE"));
        attributes.add(new DynamicAttributeMapping("MAX_ALTITUDE", AttributeType.INTEGER, "MAX_ALTITUDE"));

        dataSourceModelMapping = new DataSourceModelMapping(123L, attributes);
    }

}
