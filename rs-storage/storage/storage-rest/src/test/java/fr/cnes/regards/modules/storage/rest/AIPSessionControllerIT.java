/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.rest;

import java.net.MalformedURLException;
import java.time.OffsetDateTime;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;

public class AIPSessionControllerIT extends AbstractAIPControllerIT {

    private final String SESSION_NAME = "Test session";

    @Test
    public void testRetrieveSessions() throws MalformedURLException {
        createSeveralAips();
        int nbAipsSession1 = 8;
        int nbDeletedAipsSession1 = 2;
        int nbErrorAipsSession1 = 2;
        int nbQueueAipsSession1 = 2;
        int nbStoredAipsSession1 = 2;
        int nbAipsSession2 = 1;
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content", Matchers.hasSize(2)));

        requestBuilderCustomizer
                .expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.id", Matchers.is(SESSION_NAME)));
        requestBuilderCustomizer
                .expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.aipsCount", Matchers.is(nbAipsSession1)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.deletedAipsCount",
                                                                       Matchers.is(nbDeletedAipsSession1)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.errorAipsCount",
                                                                       Matchers.is(nbErrorAipsSession1)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.queuedAipsCount",
                                                                       Matchers.is(nbQueueAipsSession1)));
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.[0].content.storedAipsCount",
                                                                       Matchers.is(nbStoredAipsSession1)));

        requestBuilderCustomizer
                .expect(MockMvcResultMatchers.jsonPath("$.content.[1].content.aipsCount", Matchers.is(nbAipsSession2)));

        performDefaultGet(AIPSessionController.TYPE_MAPPING, requestBuilderCustomizer,
                          "we should have the list of session");
    }

    @Test
    public void testRetrieveSession() throws MalformedURLException {
        createSeveralAips();
        int nbAipsSession1 = 8;
        RequestBuilderCustomizer requestBuilderCustomizer = customizer().expectStatusOk();
        requestBuilderCustomizer.expect(MockMvcResultMatchers.jsonPath("$.content.id", Matchers.is(SESSION_NAME)));
        requestBuilderCustomizer
                .expect(MockMvcResultMatchers.jsonPath("$.content.aipsCount", Matchers.is(nbAipsSession1)));

        performDefaultGet(AIPSessionController.TYPE_MAPPING + AIPSessionController.ID_PATH, requestBuilderCustomizer,
                          "we should have the list of session", SESSION_NAME);
    }

    public void createSeveralAips() throws MalformedURLException {
        aipSessionRepo.deleteAll();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION_NAME);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);

        // Create some AIP having errors
        AIP aipOnError1 = getNewAip(aipSession);
        aipOnError1.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError1, aipSession);

        AIP aipOnError2 = getNewAip(aipSession);
        aipOnError2.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError2, aipSession);

        // Create some waiting AIP
        AIP aipWaiting1 = getNewAip(aipSession);
        aipWaiting1.setState(AIPState.STORING_METADATA);
        aipDao.save(aipWaiting1, aipSession);

        AIP aipWaiting3 = getNewAip(aipSession);
        aipWaiting3.setState(AIPState.VALID);
        aipDao.save(aipWaiting3, aipSession);

        // create some AIP already stored
        AIP aipStored1 = getNewAip(aipSession);
        aipStored1.setState(AIPState.STORED);
        aipDao.save(aipStored1, aipSession);

        AIP aipStored2 = getNewAip(aipSession);
        aipStored2.setState(AIPState.STORED);
        aipDao.save(aipStored2, aipSession);

        // create some AIP already deleted
        AIP aipDeleted1 = getNewAip(aipSession);
        aipDeleted1.setState(AIPState.DELETED);
        aipDao.save(aipDeleted1, aipSession);

        AIP aipDeleted2 = getNewAip(aipSession);
        aipDeleted2.setState(AIPState.DELETED);
        aipDao.save(aipDeleted2, aipSession);

        // Create some AIP on another session

        AIPSession aipSession2 = new AIPSession();
        aipSession2.setId("Test session 2");
        aipSession2.setLastActivationDate(OffsetDateTime.now());
        aipSession2 = aipSessionRepo.save(aipSession2);
        AIP aipDeleted11 = getNewAip(aipSession2);
        aipDeleted11.setState(AIPState.DELETED);
        aipDao.save(aipDeleted11, aipSession2);

    }

}
