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

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends notifications to collect statistics about feature extraction requests.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionNotifier {

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

    public void incrementRequestCount(FeatureExtractionRequest request) {
        incrementCount(request, SessionExtractionPropertyEnum.TOTAL_REQUESTS, 1);
    }

    // Error requests

    public void incrementRequestErrors(FeatureExtractionRequest request) {
        incrementCount(request, SessionExtractionPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementRequestErrors(FeatureExtractionRequest request) {
        decrementCount(request, SessionExtractionPropertyEnum.REQUESTS_ERRORS, 1);
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
    private void incrementCount(FeatureExtractionRequest request, SessionExtractionPropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, request.getMetadata().getSessionOwner(),
                                             request.getMetadata().getSession(),
                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                  property.getName(), String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    private void incrementCount(String source, String session, SessionExtractionPropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source,
                                             session, new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                  property.getName(), String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    // DEC
    private void decrementCount(FeatureExtractionRequest request, SessionExtractionPropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, request.getMetadata().getSessionOwner(),
                                             request.getMetadata().getSession(),
                                             new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                  property.getName(), String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.decrement(step);
    }

    private void decrementCount(String source, String session, SessionExtractionPropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source,
                                             session, new StepPropertyInfo(StepTypeEnum.ACQUISITION, property.getState(),
                                                                           property.getName(), String.valueOf(nbProducts),
                                                                           property.isInputRelated(),
                                                                           property.isOutputRelated()));
        sessionNotificationClient.decrement(step);
    }
}