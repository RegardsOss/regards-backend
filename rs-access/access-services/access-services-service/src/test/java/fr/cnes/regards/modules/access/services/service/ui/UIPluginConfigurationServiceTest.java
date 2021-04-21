/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.access.services.service.ui;

import java.util.stream.Stream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.modules.access.services.dao.ui.ILinkUIPluginsDatasetsRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginConfigurationRepository;
import fr.cnes.regards.modules.access.services.dao.ui.IUIPluginDefinitionRepository;
import fr.cnes.regards.modules.access.services.domain.ui.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.dam.domain.entities.Dataset;
import fr.cnes.regards.modules.model.domain.Model;

/**
 * Unit Test for {@link UIPluginConfigurationService}
 *
 * @author Xavier-Alexandre Brochard
 */
public class UIPluginConfigurationServiceTest {

    /**
     * Service under test
     */
    private IUIPluginConfigurationService pluginConfigurationService;

    private IUIPluginDefinitionRepository pluginRepository;

    private ILinkUIPluginsDatasetsRepository linkedUiPluginRespository;

    private IUIPluginConfigurationRepository repository;

    private IPublisher publisher;

    private IRolesClient rolesClient;

    private IAuthenticationResolver authResolver;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        pluginRepository = Mockito.mock(IUIPluginDefinitionRepository.class);
        linkedUiPluginRespository = Mockito.mock(ILinkUIPluginsDatasetsRepository.class);
        repository = Mockito.mock(IUIPluginConfigurationRepository.class);
        publisher = Mockito.mock(IPublisher.class);
        rolesClient = Mockito.mock(IRolesClient.class);
        authResolver = Mockito.mock(IAuthenticationResolver.class);

        pluginConfigurationService = new UIPluginConfigurationService(pluginRepository, linkedUiPluginRespository,
                repository, publisher, rolesClient, authResolver);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.access.services.service.ui.UIPluginConfigurationService#deletePluginconfiguration(fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration)}.
     * @throws EntityException
     */
    @Test
    public final void testDeletePluginconfiguration_shouldRemoveAndSaveLink() throws EntityException {
        UIPluginConfiguration pluginConfiguration0 = new UIPluginConfiguration();
        UIPluginConfiguration pluginConfiguration1 = new UIPluginConfiguration();
        pluginConfiguration0.setId(4334L);
        pluginConfiguration1.setId(8484L);
        Dataset dataset = new Dataset(new Model(), "tenant", "providerId", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                Lists.newArrayList(pluginConfiguration0, pluginConfiguration1));

        // Mock
        Mockito.when(repository.existsById(Mockito.anyLong())).thenReturn(true);
        Mockito.when(linkedUiPluginRespository.findAllByServicesContaining(pluginConfiguration0))
                .thenReturn(Stream.of(link));

        // Perform test
        pluginConfigurationService.deletePluginconfiguration(pluginConfiguration0);

        // Verify
        Mockito.verify(linkedUiPluginRespository).save(link);
        Mockito.verify(repository).delete(pluginConfiguration0);

    }

    /**
     * Test method for
     * {@link fr.cnes.regards.modules.access.services.service.ui.UIPluginConfigurationService#deletePluginconfiguration(fr.cnes.regards.modules.access.services.domain.ui.UIPluginConfiguration)}.
     * @throws EntityException
     */
    @Test
    public final void testDeletePluginconfiguration_shouldRemoveAndDeleteLink() throws EntityException {
        UIPluginConfiguration pluginConfiguration0 = new UIPluginConfiguration();
        pluginConfiguration0.setId(4334L);
        Dataset dataset = new Dataset(new Model(), "tenant", "providerId", "label");
        LinkUIPluginsDatasets link = new LinkUIPluginsDatasets(dataset.getIpId().toString(),
                Lists.newArrayList(pluginConfiguration0));

        // Mock
        Mockito.when(repository.existsById(Mockito.anyLong())).thenReturn(true);
        Mockito.when(linkedUiPluginRespository.findAllByServicesContaining(pluginConfiguration0))
                .thenReturn(Stream.of(link));

        // Perform test
        pluginConfigurationService.deletePluginconfiguration(pluginConfiguration0);

        // Verify
        Mockito.verify(linkedUiPluginRespository).delete(link);
        Mockito.verify(repository).delete(pluginConfiguration0);

    }

}
