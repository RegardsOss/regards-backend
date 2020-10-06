/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.rest;

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IDumpConfigurationRepository;
import fr.cnes.regards.modules.ingest.domain.dump.DumpConfiguration;
import fr.cnes.regards.modules.ingest.service.dump.IAIPMetadataService;

/**
 * {@link DumpController} REST API test
 * @author Iliana Ghazali
 */
@RegardsTransactional
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=dump_it", "regards.dump.location=target/dump" })
@ActiveProfiles(value = { "default", "test" }, inheritProfiles = false)
public class DumpControllerIT extends AbstractRegardsTransactionalIT {

    @Autowired
    IAIPMetadataService aipMetadataServiceRefactor;

    @Autowired
    IDumpConfigurationRepository dumpRepository;

    @Test
    @Purpose("Test reset last req dump date")
    public void testResetLastReqDumpDate() {
        // Create lastReqDumpDate to now
        dumpRepository.save(new DumpConfiguration(true, "", "target/", OffsetDateTime.now()));
        DumpConfiguration resource = dumpRepository.getOne(DumpConfiguration.DUMP_CONF_ID);
        Assert.assertFalse(resource.getLastDumpReqDate() == null);

        // Reset lastReqDumpDate
        RequestBuilderCustomizer putRequest = customizer().expectStatusOk();
        performDefaultPatch(DumpController.TYPE_MAPPING + DumpController.RESET_LAST_DUMP_DATE,null  , putRequest, "Reset lastReqDumpDate error");

        resource = dumpRepository.getOne(DumpConfiguration.DUMP_CONF_ID);
        Assert.assertTrue(resource.getLastDumpReqDate() == null);
    }
}

