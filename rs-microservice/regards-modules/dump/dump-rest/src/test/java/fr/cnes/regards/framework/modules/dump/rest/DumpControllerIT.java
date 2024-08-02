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


package fr.cnes.regards.framework.modules.dump.rest;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.modules.dump.service.scheduler.AbstractDumpScheduler;
import fr.cnes.regards.framework.modules.dump.service.settings.IDumpSettingsService;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;

/**
 * {@link DumpController} REST API test
 *
 * @author Iliana Ghazali
 */

@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=rest_dump_it" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class DumpControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    IDumpSettingsService dumpSettingsService;

    @MockBean
    private AbstractDumpScheduler abstractDumpScheduler;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(AbstractDumpScheduler.class);
    }

    @Test
    @Purpose("Test reset last req dump date")
    public void testResetLastReqDumpDate() throws EntityException {

        // Create lastReqDumpDate to now
        dumpSettingsService.setLastDumpReqDate(OffsetDateTime.now());
        Assert.assertNotNull(dumpSettingsService.lastDumpReqDate());

        // Reset lastReqDumpDate
        RequestBuilderCustomizer putRequest = customizer().expectStatusOk();
        performDefaultPatch(DumpController.TYPE_MAPPING + DumpController.RESET_LAST_DUMP_DATE,
                            null,
                            putRequest,
                            "Reset lastReqDumpDate error");

        Assert.assertNull(dumpSettingsService.lastDumpReqDate());
    }

}
