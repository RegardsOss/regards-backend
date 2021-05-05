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
package fr.cnes.regards.framework.modules.session.management.rest;

import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.modules.session.commons.domain.events.SourceDeleteEvent;
import fr.cnes.regards.framework.modules.session.management.dao.ISourceRepository;
import fr.cnes.regards.framework.modules.session.management.domain.Source;
import fr.cnes.regards.framework.modules.session.management.service.controllers.SourceService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for {@link SourceController}
 *
 * @author Iliana Ghazali
 **/
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=source_controller_it" })
public class SourceControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISourceRepository sourceRepo;

    @Autowired
    private SourceService sourceService;

    @SpyBean
    protected IPublisher publisher;

    private static final String SOURCE_1 = "SOURCE1";

    private static final String SOURCE_2 = "SOURCE2";

    private static final String SOURCE_3 = "SOURCE3";

    private static final String SOURCE_4 = "SOURCE4";

    private static final String SOURCE_5 = "SOURCE5";

    @Before
    public void init() {
        this.tenantResolver.forceTenant(getDefaultTenant());
        this.sourceRepo.deleteAll();
        createSources();
        Mockito.clearInvocations(publisher);
    }

    @Test
    @Purpose("Check if sources are correctly retrieved according to the filters")
    public void getSourcesTest() {
        // return all sources
        performDefaultGet(SourceController.ROOT_MAPPING,
                          customizer().expectStatusOk().expectValue("$.metadata.totalElements", 5),
                          "Wrong number of sources returned");

        // Search the first source by name
        RequestBuilderCustomizer customizer1 = customizer();
        customizer1.addParameter("name", SOURCE_1);
        customizer1.expectStatusOk();
        customizer1.expectValue("$.metadata.totalElements", 1);
        customizer1.expectValue("$.content.[0].content.name", SOURCE_1);
        performDefaultGet(SourceController.ROOT_MAPPING, customizer1, "The source expected was not returned");

        // Search source by state = running
        RequestBuilderCustomizer customizer2 = customizer();
        customizer2.addParameter("state", "running");
        customizer2.expectStatusOk();
        customizer2.expectValue("$.metadata.totalElements", 1);
        customizer2.expectValue("$.content.[0].content.name", SOURCE_3);
        performDefaultGet(SourceController.ROOT_MAPPING, customizer2, "The source expected was not returned");

        // Search source by state = error
        RequestBuilderCustomizer customizer3 = customizer();
        customizer3.addParameter("state", "error");
        customizer3.expectStatusOk();
        customizer3.expectValue("$.metadata.totalElements", 1);
        customizer3.expectValue("$.content.[0].content.name", SOURCE_1);
        performDefaultGet(SourceController.ROOT_MAPPING, customizer3, "The source expected was not returned");

        // Search source by state = waiting
        RequestBuilderCustomizer customizer4 = customizer();
        customizer4.addParameter("state", "waiting");
        customizer4.expectStatusOk();
        customizer4.expectValue("$.metadata.totalElements", 1);
        customizer4.expectValue("$.content.[0].content.name", SOURCE_2);
        performDefaultGet(SourceController.ROOT_MAPPING, customizer4, "The source expected was not returned");

        // Search source by state = ok
        RequestBuilderCustomizer customizer5 = customizer();
        customizer5.addParameter("state", "ok");
        customizer5.expectStatusOk();
        customizer5.expectValue("$.metadata.totalElements", 2);
        performDefaultGet(SourceController.ROOT_MAPPING, customizer5, "The sources expected were not returned");
    }

    @Test
    @Purpose("Test the deletion of a source")
    public void deleteSource() {
        performDefaultDelete(SourceController.ROOT_MAPPING + SourceController.DELETE_SOURCE_MAPPING,
                             customizer().expectStatusOk(), "The order to delete a source was not published", SOURCE_1);

        Mockito.verify(publisher, Mockito.times(1)).publish(Mockito.any(SourceDeleteEvent.class));
    }

    @Test
    @Purpose("Test the deletion of a not existing source")
    public void deleteNotExistingSource() {
        performDefaultDelete(SourceController.ROOT_MAPPING + SourceController.DELETE_SOURCE_MAPPING,
                             customizer().expectStatus(HttpStatus.NOT_FOUND),
                             "The order to delete a" + "source was published but the source does not exist",
                             "NOT_EXISTING");

        Mockito.verify(publisher, Mockito.times(0)).publish(Mockito.any(SourceDeleteEvent.class));
    }

    /**
     * Init sources
     */
    public void createSources() {
        Set<Source> sourceSet = new HashSet<>();

        // create sources
        Source source1 = new Source(SOURCE_1);
        source1.getManagerState().setErrors(true);
        sourceSet.add(source1);

        Source source2 = new Source(SOURCE_2);
        source2.getManagerState().setWaiting(true);
        sourceSet.add(source2);

        Source source3 = new Source(SOURCE_3);
        source3.getManagerState().setRunning(true);
        sourceSet.add(source3);

        Source source4 = new Source(SOURCE_4);
        sourceSet.add(source4);

        Source source5 = new Source(SOURCE_5);
        sourceSet.add(source5);

        this.sourceRepo.saveAll(sourceSet);
    }
}