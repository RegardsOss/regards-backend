/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.entities.urn.OAISIdentifier;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import fr.cnes.regards.modules.models.domain.EntityType;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource(locations = "classpath:test.properties")
@MultitenantTransactional
public class LinkDatasetsPluginsControllerIT extends AbstractRegardsTransactionalIT {

    private static final Logger LOG = LoggerFactory.getLogger(LinkDatasetsPluginsControllerIT.class);

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_230")
    @Purpose("The system allows to get the list of plugin's service for a dataset")
    public void retrieveLink() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        UniformResourceName urn=new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, DEFAULT_TENANT,
                                                        UUID.randomUUID(),1);
        performDefaultGet(LinkPluginsDatasetsController.PATH_LINK, expectations,
                          "Failed to fetch a specific dataset using its id", urn.toString());
    }

    @Test
    @Requirement("REGARDS_DSL_DAM_SET_210")
    @Purpose("The system allows to link a plugin's service to a dataset")
    public void updateLink() {
        final List<ResultMatcher> expectations = new ArrayList<>();
        expectations.add(MockMvcResultMatchers.status().isOk());
        expectations.add(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE));
        UniformResourceName urn=new UniformResourceName(OAISIdentifier.AIP, EntityType.DATASET, DEFAULT_TENANT,
                                                        UUID.randomUUID(),1);
        final LinkPluginsDatasets newLink = new LinkPluginsDatasets(urn.toString(), Sets.newHashSet());
        performDefaultPut(LinkPluginsDatasetsController.PATH_LINK, newLink, expectations,
                          "Failed to fetch a specific dataset using its id", urn.toString());
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

}
