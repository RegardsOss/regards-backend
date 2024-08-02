/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.catalog.services.rest;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.accessrights.client.ILicenseClient;
import fr.cnes.regards.modules.catalog.services.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.storage.client.IStorageRestClient;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

/**
 * @author Sylvain Vissiere-Guerinet
 */
@TestPropertySource(locations = "classpath:test.properties")
@MultitenantTransactional
public class LinkDatasetsPluginsControllerIT extends AbstractRegardsTransactionalIT {

    @MockBean
    private ILicenseClient licenseClient;

    @MockBean
    private IStorageRestClient storageRestClient;

    private static final Logger LOG = LoggerFactory.getLogger(LinkDatasetsPluginsControllerIT.class);

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_230")
    @Purpose("The system allows to get the list of plugin's service for a dataset")
    public void retrieveLink() {
        UniformResourceName urn = UniformResourceName.build(OAISIdentifier.AIP.name(),
                                                            EntityType.DATASET,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1,
                                                            null,
                                                            null);
        performDefaultGet(LinkPluginsDatasetsController.PATH_LINK,
                          customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_UTF8_VALUE),
                          "Failed to fetch a specific dataset using its id",
                          urn.toString());
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_210")
    @Purpose("The system allows to link a plugin's service to a dataset")
    public void updateLink() {
        UniformResourceName urn = UniformResourceName.build(OAISIdentifier.AIP.name(),
                                                            EntityType.DATASET,
                                                            getDefaultTenant(),
                                                            UUID.randomUUID(),
                                                            1,
                                                            null,
                                                            null);
        final LinkPluginsDatasets newLink = new LinkPluginsDatasets(urn.toString(), Sets.newHashSet());
        performDefaultPut(LinkPluginsDatasetsController.PATH_LINK,
                          newLink,
                          customizer().expectStatusOk().expectContentType(MediaType.APPLICATION_JSON_UTF8_VALUE),
                          "Failed to fetch a specific dataset using its id",
                          urn.toString());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
