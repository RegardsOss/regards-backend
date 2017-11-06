/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service;

import org.junit.Test;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * @author Sébastien Binda
 */
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
public class SipServiceTest {

    @Requirement("REGARDS_DSL_ING_PRO_535")
    @Requirement("REGARDS_DSL_ING_PRO_610")
    @Requirement("REGARDS_DSL_ING_PRO_620")
    @Requirement("REGARDS_DSL_ING_PRO_630")
    @Requirement("REGARDS_DSL_ING_PRO_650")
    @Requirement("REGARDS_DSL_ING_PRO_660")
    @Purpose("Manage SIP deletion by ipId")
    @Test
    public void deleteByIpId() {
        // TODO : Check deletion of one sip
        // TODO : Check SIP Event
        // TODO : Check AIP deletion events
    }

    @Requirement("REGARDS_DSL_ING_PRO_535")
    @Requirement("REGARDS_DSL_ING_PRO_640")
    @Requirement("REGARDS_DSL_ING_PRO_660")
    @Purpose("Manage SIP deletion by sipId")
    @Test
    public void deleteBySipId() {
        // TODO : Check deletion of all sips
        // TODO : Check SIP Event
        // TODO : Check AIP deletion events
        // TODO : Check for deletion date
    }

    @Requirement("REGARDS_DSL_ING_PRO_520")
    @Purpose("Search for SIP by state")
    @Test
    public void searchSip() {
        // TODO : Check search by state

    }

    @Requirement("REGARDS_DSL_ING_PRO_550")
    @Purpose("Manage indexed SIP")
    @Test
    public void indexSip() {
        // TODO : Check that if all AIPs are indexed then the SIP is in INDEXED state
    }

    @Requirement("REGARDS_DSL_ING_PRO_710")
    @Requirement("REGARDS_DSL_ING_PRO_720")
    @Requirement("REGARDS_DSL_ING_PRO_740")
    @Requirement("REGARDS_DSL_ING_PRO_750")
    @Requirement("REGARDS_DSL_ING_PRO_760")
    @Purpose("Manage sessions informations")
    @Test
    public void checkSessions() {
        // TODO : Check session id
        // TODO : Check session progress
        // TODO : Check session state ("STORED" if all SIP are in "STORED" state) ("INDEXED" if all SIP are "INDEXED")
        // TODO : Check all associated SIP state.
    }

    @Requirement("REGARDS_DSL_ING_PRO_830")
    @Requirement("REGARDS_DSL_ING_PRO_810")
    @Purpose("Manage session deletion")
    @Test
    public void deleteSession() {
        // TODO : Delete by id
        // TODO : Check assoatec SIP and AIP deletion
    }

}
