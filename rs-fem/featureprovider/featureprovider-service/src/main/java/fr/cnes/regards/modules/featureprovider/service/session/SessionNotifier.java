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
package fr.cnes.regards.modules.featureprovider.service.session;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends notifications to collect statistics about feature extraction process.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionNotifier {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionNotifier.class);

    /**
     * Name of the corresponding SessionStep
     */
    private static final String GLOBAL_SESSION_STEP = "extract";

    /**
     * Service to notify property changes
     */
    @Autowired
    private ISessionAgentClient sessionNotificationClient;

    // Extraction requests received

    public void incrementRequestCount(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionExtractionPropertyEnum.TOTAL_REQUESTS, 1);
    }

    // Error requests

    public void incrementRequestErrors(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionExtractionPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementRequestErrors(String sessionOwner, String session, long nbProducts) {
        decrementCount(sessionOwner, session, SessionExtractionPropertyEnum.REQUESTS_ERRORS, nbProducts);
    }

    // Refused requests

    public void incrementRequestRefused(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionExtractionPropertyEnum.REQUESTS_REFUSED, 1);
    }

    public void decrementRequestRefused(String sessionOwner, String session, long nbProducts) {
        decrementCount(sessionOwner, session, SessionExtractionPropertyEnum.REQUESTS_REFUSED, nbProducts);
    }

    // Generated products

    public void incrementGeneratedProducts(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionExtractionPropertyEnum.GENERATED_PRODUCTS, 1);
    }

    public void decrementGeneratedProducts(String sessionOwner, String session) {
        decrementCount(sessionOwner, session, SessionExtractionPropertyEnum.GENERATED_PRODUCTS, 1);
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC

    /**
     * Send an INC event to {@link ISessionAgentClient}
     *
     * @param source     also called sessionOwner, originator of the request
     * @param session    tags the data processed with the same name
     * @param property   property to be notified
     * @param nbProducts value to increment the corresponding property
     */
    private void incrementCount(String source, String session, SessionExtractionPropertyEnum property,
            long nbProducts) {
        if (!Strings.isNullOrEmpty(source) && !Strings.isNullOrEmpty(session)) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                                 new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                      property.getName(), String.valueOf(nbProducts),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionNotificationClient.increment(step);
        } else {
            LOGGER.debug(
                    "Session has not been incremented of {} features because either sessionOwner({}) or session({}) is null or empty",
                    nbProducts, source, session);
        }
    }

    // DEC

    /**
     * Send an DEC event to {@link ISessionAgentClient}
     *
     * @param source     also called sessionOwner, originator of the request
     * @param session    tags the data processed with the same name
     * @param property   property to be notified
     * @param nbProducts value to decrement the corresponding property
     */
    private void decrementCount(String source, String session, SessionExtractionPropertyEnum property,
            long nbProducts) {
        if (!Strings.isNullOrEmpty(source) && !Strings.isNullOrEmpty(session)) {

            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                                 new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                      property.getName(), String.valueOf(nbProducts),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionNotificationClient.decrement(step);
        } else {
            LOGGER.debug(
                    "Session has not been decremented of {} features because either sessionOwner({}) or session({}) "
                            + "is null or empty", nbProducts, source, session);
        }
    }
}