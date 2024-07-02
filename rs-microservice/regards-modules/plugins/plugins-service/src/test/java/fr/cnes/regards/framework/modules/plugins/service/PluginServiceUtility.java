/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.plugins.service;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.encryption.BlowfishEncryptionService;
import fr.cnes.regards.framework.encryption.configuration.CipherProperties;
import fr.cnes.regards.framework.modules.plugins.SamplePlugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.AbstractPluginParam;
import fr.cnes.regards.framework.modules.plugins.dto.parameter.parameter.IPluginParam;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import org.junit.Before;
import org.mockito.Mockito;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Constants and datas for unit testing of plugin's Service.
 *
 * @author Christophe Mertz
 */
public class PluginServiceUtility {

    protected PluginConfigurationService pluginDaoServiceMocked;

    protected IPluginService pluginServiceMocked;

    protected IPublisher publisherMocked;

    protected IRuntimeTenantResolver runtimeTenantResolver;

    public static final String A_SAMPLE_PLUGIN_PLUGIN_ID = "aSamplePlugin";

    /**
     * Project used for test
     */
    protected static final String PROJECT = "test1";

    /**
     * An id constant {@link String}
     */
    protected static final Long AN_ID = 33L;
    //    protected static final String VERSION = "12345-6789-11";

    /**
     * Version
     */
    protected static final String VERSION = "0.0.1";

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
     * 5 constant {@link String}
     */
    protected static final int CINQ = 5;

    /**
     * 4 constant {@link String}
     */
    protected static final int QUATRE = 4;

    /**
     * A plugin identifier constant {@link String}
     */
    protected static final String PLUGIN_PARAMETER_ID = "aParameterPlugin";

    /**
     * A {@link List} of values
     */
    protected static final Set<String> DYNAMICVALUES = Stream.of(RED, BLUE, GREEN).collect(Collectors.toSet());

    /**
     * A {@link AbstractPluginParam}
     */
    protected static final Set<IPluginParam> DYNAMICPARAMETERS = IPluginParam.set(IPluginParam.build("param11",
                                                                                                     "value11"),
                                                                                  IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                                                     0).dynamic(),
                                                                                  IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                                                     Boolean.TRUE),
                                                                                  IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                                                     RED)
                                                                                              .dynamic(DYNAMICVALUES));

    /**
     * A {@link AbstractPluginParam}
     */
    protected static final Set<IPluginParam> DYNAMICPARAMETERS_TO_UPDATE = IPluginParam.set(IPluginParam.build("param11",
                                                                                                               "value11"),
                                                                                            IPluginParam.build(
                                                                                                SamplePlugin.FIELD_NAME_COEF,
                                                                                                0).dynamic(),
                                                                                            IPluginParam.build(
                                                                                                SamplePlugin.FIELD_NAME_ACTIVE,
                                                                                                Boolean.TRUE),
                                                                                            IPluginParam.build(
                                                                                                            SamplePlugin.FIELD_NAME_SUFFIX,
                                                                                                            RED)
                                                                                                        .dynamic(
                                                                                                            DYNAMICVALUES));

    /**
     * A list of {@link AbstractPluginParam}
     */
    protected static final Set<IPluginParam> INTERFACEPARAMETERS = IPluginParam.set(IPluginParam.build("param31",
                                                                                                       "value31"),

                                                                                    IPluginParam.build("param32",
                                                                                                       "value32"),
                                                                                    IPluginParam.build("param33",
                                                                                                       "value33"),
                                                                                    IPluginParam.build("param34",
                                                                                                       "value34"),
                                                                                    IPluginParam.build("param35",
                                                                                                       "value35"),
                                                                                    IPluginParam.build(SamplePlugin.FIELD_NAME_COEF,
                                                                                                       3).dynamic(),
                                                                                    IPluginParam.build(SamplePlugin.FIELD_NAME_ACTIVE,
                                                                                                       Boolean.TRUE),
                                                                                    IPluginParam.build(SamplePlugin.FIELD_NAME_SUFFIX,
                                                                                                       "Toulouse"));

    /**
     * A {@link PluginConfiguration}
     */
    private final PluginConfiguration pluginConfiguration1 = new PluginConfiguration(
        "a configuration from PluginServiceUtility",
        INTERFACEPARAMETERS,
        0,
        A_SAMPLE_PLUGIN_PLUGIN_ID);

    /**
     * A list of {@link AbstractPluginParam} with a dynamic {@link AbstractPluginParam}.
     */
    private final PluginConfiguration pluginConfiguration2 = new PluginConfiguration(
        "second configuration from PluginServiceUtility",
        DYNAMICPARAMETERS,
        0,
        A_SAMPLE_PLUGIN_PLUGIN_ID);

    /**
     * A list of {@link AbstractPluginParam} without parameters.
     */
    private final PluginConfiguration pluginConfiguration3 = new PluginConfiguration(
        "third configuration from PluginServiceUtility",
        CINQ,
        A_SAMPLE_PLUGIN_PLUGIN_ID);

    /**
     * A list of {@link AbstractPluginParam} with a dynamic {@link AbstractPluginParam}.
     */
    private final PluginConfiguration pluginConfiguration4 = new PluginConfiguration("fourth configuration",
                                                                                     DYNAMICPARAMETERS_TO_UPDATE,
                                                                                     0,
                                                                                     A_SAMPLE_PLUGIN_PLUGIN_ID);

    /**
     * This method is run before all tests
     */
    @Before
    public void init() throws InvalidAlgorithmParameterException, InvalidKeyException, IOException {
        runtimeTenantResolver = Mockito.mock(IRuntimeTenantResolver.class);
        Mockito.when(runtimeTenantResolver.getTenant()).thenReturn("tenant");

        // mock services
        publisherMocked = Mockito.mock(IPublisher.class);
        pluginDaoServiceMocked = Mockito.mock(PluginConfigurationService.class);
        BlowfishEncryptionService blowfishEncryptionService = new BlowfishEncryptionService();
        blowfishEncryptionService.init(new CipherProperties("src/test/resources/testKey", "12345678"));
        PluginInstantiationService pluginInstanceService = new PluginInstantiationService(pluginDaoServiceMocked,
                                                                                          blowfishEncryptionService);
        // init plugin service
        pluginServiceMocked = new PluginService(pluginDaoServiceMocked,
                                                publisherMocked,
                                                runtimeTenantResolver,
                                                new PluginCache(pluginInstanceService, pluginDaoServiceMocked, runtimeTenantResolver),
                                                pluginInstanceService,
                                                blowfishEncryptionService,
                                                null);
        PluginUtils.setup(Arrays.asList("fr.cnes.regards.plugins",
                                        "fr.cnes.regards.framework.plugins",
                                        "fr.cnes.regards.framework.modules.plugins"));
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

    protected PluginConfiguration getPluginConfigurationWithoutParametersToUpdate() {
        return pluginConfiguration4;
    }

    protected void resetId() {
        getPluginConfigurationWithDynamicParameter().setId(null);
        getPluginConfigurationWithParameters().setId(null);
    }

}
