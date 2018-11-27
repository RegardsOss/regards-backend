/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;

/***
 * Constants and datas for unit testing of plugin's Domain.
 *
 * @author Christophe Mertz
 */
public class PluginDomainUtility {

    /**
     * Project used for test
     */
    protected static final String PROJECT = "test1";

    /**
     * An id constant {@link Long}
     */
    protected static final Long AN_ID = new Long(33);

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

    /**
     * HELLO constant {@link String}
     */
    protected static final String HELLO = "hello";

    /**
     * RESULT constant {@link String}
     */
    protected static final String RESULT = "result=";

    /**
     * 5 constant {@link int}
     */
    protected static final int CINQ = 5;

    /**
     * 4 constant {@link int}
     */
    protected static final int QUATRE = 4;

    /**
     * isActive constant {@link String}
     */
    protected static final String PARAM_IS_ACTIVE = "isBoolean";

    /**
     * A plugin identifier constant {@link String}
     */
    protected static final String PLUGIN_PARAMETER_ID = "aParameterPlugin";

    /**
     * A {@link List} of values
     */
    protected static final List<String> DYNAMICVALUES = Arrays.asList(RED, BLUE, GREEN);

    /**
     * A {@link PluginParameter}
     */
    protected static final Set<PluginParameter> DYNAMICPARAMETERS = PluginParametersFactory.build()
            .addParameter("param11", "value11").addDynamicParameter("coeff", "0")
            .addParameter(PARAM_IS_ACTIVE, Boolean.TRUE.toString()).addDynamicParameter("suffix", RED, DYNAMICVALUES)
            .getParameters();

    /**
     * A list of {@link PluginParameter}
     */
    protected static final Set<PluginParameter> INTERFACEPARAMETERS = PluginParametersFactory.build()
            .addParameter("param31", "value31").addParameter("param32", "value32").addParameter("param33", "value33")
            .addParameter("param34", "value34").addParameter("param35", "value35").addDynamicParameter("Koeff", "3")
            .addParameter(PARAM_IS_ACTIVE, Boolean.TRUE.toString()).addParameter("suffixe", "Toulouse").getParameters();

    /**
     * A {@link PluginConfiguration}
     */
    private final PluginConfiguration pluginConfiguration1 = new PluginConfiguration(getPluginMetaData(),
                                                                                     "a configuration from PluginDomainUtility",
                                                                                     INTERFACEPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} with a dynamic {@link PluginParameter}
     */
    private final PluginConfiguration pluginConfiguration2 = new PluginConfiguration(getPluginMetaData(),
                                                                                     "second configuration  from PluginDomainUtility",
                                                                                     DYNAMICPARAMETERS, 0);

    /**
     * A list of {@link PluginParameter} without parameters.
     */
    private final PluginConfiguration pluginConfiguration3 = new PluginConfiguration(getPluginMetaData(),
                                                                                     "third configuration from PluginDomainUtility",
                                                                                     QUATRE);

    protected PluginMetaData getPluginMetaData() {
        final PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName(Integer.class.getCanonicalName());
        pluginMetaData.setInterfaceNames(Sets.newHashSet("TestInterface"));
        pluginMetaData.setPluginId("aSamplePlugin");
        pluginMetaData.setAuthor("CS-SI");
        pluginMetaData.setVersion(VERSION);
        return pluginMetaData;
    }

    protected PluginConfiguration getPluginConfigurationWithParameters() {
        return pluginConfiguration1;
    }

    protected PluginConfiguration getPluginConfigurationWithDynamicParameter() {
        return pluginConfiguration2;
    }

    protected PluginConfiguration getPluginConfigurationWithoutParameters() {
        return pluginConfiguration3;
    }

    protected void resetId() {
        getPluginConfigurationWithDynamicParameter().setId(null);
        getPluginConfigurationWithDynamicParameter().getParameters().forEach(p -> p.setId(null));
        getPluginConfigurationWithParameters().setId(null);
        getPluginConfigurationWithParameters().getParameters().forEach(p -> p.setId(null));
    }

}
