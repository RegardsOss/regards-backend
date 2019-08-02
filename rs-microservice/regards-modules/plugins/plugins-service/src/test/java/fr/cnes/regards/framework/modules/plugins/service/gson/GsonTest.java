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
package fr.cnes.regards.framework.modules.plugins.service.gson;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.CollectionPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonCollectionPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonMapPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.JsonObjectPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.MapPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.ObjectPluginParam;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;

/**
 * IT test class for IPluginService
 * @author Marc SORDI
 */
@MultitenantTransactional
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=plugin_test_db")
public class GsonTest extends AbstractMultitenantServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonTest.class);

    @Autowired
    private Gson gson;

    @Before
    public void before() {
        PluginParameterTransformer.setup(gson);
    }

    @Test
    public void testPojoSerialization() throws JsonSyntaxException, ClassNotFoundException {
        PluginConfiguration conf = readJsonContract("plugin_conf_pojo.json");
        Assert.assertNotNull(conf);

        // Tranform JSON to POJO
        ObjectPluginParam opp = PluginParameterTransformer
                .transform((JsonObjectPluginParam) conf.getParameter("simple"));
        Assert.assertNotNull(opp);
        Assert.assertTrue(SimplePojo.class.isInstance(opp.getValue()));
    }

    @Test
    public void testMapSerialization() throws JsonSyntaxException, ClassNotFoundException {
        PluginConfiguration conf = readJsonContract("plugin_conf_map.json");
        Assert.assertNotNull(conf);

        // Tranform JSON to POJO
        MapPluginParam mpp = PluginParameterTransformer.transform((JsonMapPluginParam) conf.getParameter("simplemap"));
        Assert.assertNotNull(mpp);
        mpp.getValue().entrySet().forEach(e -> Assert.assertTrue(SimplePojo.class.isInstance(e.getValue())));
    }

    @Test
    public void testCollectionSerialization() throws JsonSyntaxException, ClassNotFoundException {
        PluginConfiguration conf = readJsonContract("plugin_conf_collection.json");
        Assert.assertNotNull(conf);

        // Tranform JSON to POJO
        CollectionPluginParam mpp = PluginParameterTransformer
                .transform((JsonCollectionPluginParam) conf.getParameter("simplelist"));
        Assert.assertNotNull(mpp);
        mpp.getValue().forEach(e -> Assert.assertTrue(SimplePojo.class.isInstance(e)));
    }

    protected PluginConfiguration readJsonContract(String filename) {
        Path contract;
        try {
            contract = Paths.get(this.getClass().getResource(filename).toURI());
        } catch (URISyntaxException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new AssertionError(e1);
        }

        if (Files.exists(contract)) {
            try (JsonReader reader = new JsonReader(new FileReader(contract.toFile()))) {
                return gson.fromJson(reader, PluginConfiguration.class);
            } catch (IOException e) {
                String message = "Cannot read JSON contract";
                LOGGER.error(message, e);
                throw new AssertionError(message, e);
            }
        } else {
            String message = String.format("File does not exist : %s", filename);
            LOGGER.error(message);
            throw new AssertionError(message);
        }
    }
}
