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
package fr.cnes.regards.framework.modules.plugins.dao;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;

/***
 * Constants and datas for unit testing of plugin's DAO.
 *
 * @author Christophe Mertz
 */
public abstract class PluginDaoUtility extends AbstractDaoTest {

    /**
     * Class logger
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(PluginDaoUtility.class);

    /**
     * Project used for test
     */
    protected static final String PROJECT = "test1";

    /**
     * Version
     */
    protected static final String VERSION = "12345-6789-11";

    /**
     * Role used for test
     */
    protected static final String USERROLE = "USERROLE";

    /**
     * RED constant {@link String}
     */
    protected static final String RED = "red";

    /**
     * GREEN constant {@link String}
     */
    protected static final String GREEN = "green";

    /**
     * BLUE constant {@link String}
     */
    protected static final String BLUE = "blue";

    /**
     * BLUE constant {@link String}
     */
    protected static final String INVALID_JWT = "Invalid JWT";

    protected static final IPluginParam ONE_PARAMETER = IPluginParam.build("param11", "value11");

    /**
     * A {@link List} of values
     */
    protected static final Set<String> DYNAMICVALUES = new HashSet<>(Arrays.asList(RED, BLUE, GREEN));

    protected static final Set<IPluginParam> LIST_PARAMETERS = IPluginParam
            .set(IPluginParam.build("param-dyn21", RED).dynamic(DYNAMICVALUES),
                 IPluginParam.build("param-dyn31", GREEN).dynamic(DYNAMICVALUES),
                 IPluginParam.build("param51", "value51"), IPluginParam.build("param61", "value61"));

    /**
     * A list of {@link AbstractPluginParam}
     */
    protected static final Set<IPluginParam> INTERFACEPARAMETERS = IPluginParam
            .set(IPluginParam.build("param41", "value41"), IPluginParam.build("param42", "value42"),
                 IPluginParam.build("param43", "value43"), IPluginParam.build("param44", "value44"),
                 IPluginParam.build("param45", "value45"));

    /**
     * IPluginConfigurationRepository
     */
    @Autowired
    protected IPluginConfigurationRepository plgRepository;

    static PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.getInterfaceNames().add("TestInterface");
        pluginMetaData.setPluginId("plugin-id");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    /**
     * Don't re-use same entity reference when a collection with delete-orphan is specified (parameters for example)
     * So create a new object each time we need one
     */
    public static PluginConfiguration getPlgConfWithParameters() {
        return new PluginConfiguration(getPluginMetaData(), "a configuration from PluginDaoUtility",
                INTERFACEPARAMETERS, 0);
    }

    /**
     * Don't re-use same entity reference when a collection with delete-orphan is specified (parameters for example)
     * So create a new object each time we need one
     */
    public static PluginConfiguration getPlgConfWithDynamicParameter() {
        return new PluginConfiguration(getPluginMetaData(), "second configuration from PluginDaoUtility",
                LIST_PARAMETERS, 0);
    }

    protected void cleanDb() {
        plgRepository.deleteAll();
    }
}
