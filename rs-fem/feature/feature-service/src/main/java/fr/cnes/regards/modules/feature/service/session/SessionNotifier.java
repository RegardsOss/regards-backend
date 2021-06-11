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
package fr.cnes.regards.modules.feature.service.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends notifications to collect statistics about feature requests.
 *
 * @author Iliana Ghazali
 **/
@Service
@MultitenantTransactional
public class SessionNotifier {

    /**
     * Name of the corresponding SessionStep
     */
    private static final String GLOBAL_SESSION_STEP = "feature";

    /**
     * Service to notify property changes
     */
    @Autowired
    private ISessionAgentClient sessionNotificationClient;

    //TODO : one method per each property : examples below

    // Error requests
    public void incrementRequestErrors(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionFeaturePropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementRequestErrors(String sessionOwner, String session, long nbProducts) {
        decrementCount(sessionOwner, session, SessionFeaturePropertyEnum.REQUESTS_ERRORS, nbProducts);
    }

    // Refused requests

    public void incrementRequestRefused(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionFeaturePropertyEnum.REQUESTS_REFUSED, 1);
    }

    public void decrementRequestRefused(String sessionOwner, String session, long nbProducts) {
        decrementCount(sessionOwner, session, SessionFeaturePropertyEnum.REQUESTS_REFUSED, nbProducts);
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC
    private void incrementCount(String source, String session, SessionFeaturePropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, property.getState(),
                                                                  property.getName(), String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.increment(step);
    }

    // DEC
    private void decrementCount(String source, String session, SessionFeaturePropertyEnum property,
            long nbProducts) {
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                             new StepPropertyInfo(StepTypeEnum.REFERENCING, property.getState(),
                                                                  property.getName(), String.valueOf(nbProducts),
                                                                  property.isInputRelated(),
                                                                  property.isOutputRelated()));
        sessionNotificationClient.decrement(step);
    }
}