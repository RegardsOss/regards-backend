/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.access.services.domain;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto;
import fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceType;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginDefinition;
import fr.cnes.regards.modules.catalog.services.domain.ServiceScope;
import fr.cnes.regards.modules.catalog.services.domain.dto.PluginConfigurationDto;

/**
 * Unit test for {@link PluginServiceDto}
 *
 * @author Xavier-Alexandre Brochard
 */
public class PluginServiceDtoTest {

    private static final Long ID = 0L;

    private static final String LABEL = "the label";

    private static URL ICON_URL;

    private static final Set<ServiceScope> APPLICATION_MODES = Sets.newHashSet(ServiceScope.ONE, ServiceScope.MANY);

    private static final Set<EntityType> ENTITY_TYPES = Sets.newHashSet(EntityType.DATA);

    @Before
    public void setUp() throws MalformedURLException {
        ICON_URL = new URL("http://www.google.com");
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto#fromPluginConfiguration(fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration)}.
     */
    @Test
    public final void testFromPluginConfiguration() {
        PluginMetaData pluginMetaData = new PluginMetaData();
        pluginMetaData.setPluginClassName("fr.cnes.regards.modules.catalog.services.plugins.SampleServicePlugin");

        PluginConfiguration pluginConfiguration = new PluginConfiguration(pluginMetaData, LABEL);
        pluginConfiguration.setId(ID);
        pluginConfiguration.setIconUrl(ICON_URL);

        PluginConfigurationDto pluginConfigurationDto = new PluginConfigurationDto(pluginConfiguration);

        PluginServiceDto pluginServiceDto = PluginServiceDto.fromPluginConfigurationDto(pluginConfigurationDto);

        checkDto(pluginServiceDto);
        Assert.assertEquals(PluginServiceType.CATALOG, pluginServiceDto.getType());
    }

    /**
     * Test method for {@link fr.cnes.regards.modules.access.services.domain.aggregator.PluginServiceDto#fromUIPluginConfiguration(fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration)}.
     */
    @Test
    public final void testFromUIPluginConfiguration() {
        UIPluginConfiguration pluginConfiguration = new UIPluginConfiguration();
        UIPluginDefinition pluginDefinition = new UIPluginDefinition();
        pluginConfiguration.setId(ID);
        pluginConfiguration.setLabel(LABEL);
        pluginConfiguration.setPluginDefinition(pluginDefinition);

        pluginDefinition.setIconUrl(ICON_URL);
        pluginDefinition.setApplicationModes(APPLICATION_MODES);
        pluginDefinition.setEntityTypes(ENTITY_TYPES);

        PluginServiceDto dto = PluginServiceDto.fromUIPluginConfiguration(pluginConfiguration);

        checkDto(dto);
        Assert.assertEquals(PluginServiceType.UI, dto.getType());
    }

    /**
     * Check the values of the DTO
     * @param pPluginServiceDto
     */
    private void checkDto(PluginServiceDto pPluginServiceDto) {
        Assert.assertEquals(ID, pPluginServiceDto.getConfigId());
        Assert.assertEquals(LABEL, pPluginServiceDto.getLabel());
        Assert.assertEquals(ICON_URL, pPluginServiceDto.getIconUrl());
        Assert.assertEquals(APPLICATION_MODES, pPluginServiceDto.getApplicationModes());
        Assert.assertEquals(ENTITY_TYPES, pPluginServiceDto.getEntityTypes());
    }
}
