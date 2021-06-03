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
package fr.cnes.regards.modules.storage.service.session;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.modules.session.agent.client.ISessionAgentClient;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepProperty;
import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyInfo;
import fr.cnes.regards.framework.modules.session.commons.domain.StepTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that sends notification to collect statistics about storage by sessionOwner and session
 *
 * @author Iliana Ghazali
 **/

@Service
@MultitenantTransactional
public class SessionNotifier {

    /**
     * Service to notify changes on steps
     */
    @Autowired
    private ISessionAgentClient sessionNotificationClient;

    /**
     * Parameters
     */
    // Name of the global step
    private static final String GLOBAL_SESSION_STEP = "storage";

    /**
     * Methods used to notify the session
     */

    // ----------- REQUESTS -----------

    // reference requests
    public void incrementReferenceRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REFERENCE_REQUESTS, 1);
    }

    // store request
    public void incrementStoreRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.STORE_REQUESTS, 1);
    }

    // copy requests
    public void incrementCopyRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.COPY_REQUESTS, 1);
    }

    // deletion requests
    public void incrementDeleteRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.DELETE_REQUESTS, 1);
    }

    // running requests
    // note : running requests are only implemented
    public void incrementRunningRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    public void decrementRunningRequests(String sessionOwner, String session) {
        decrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_RUNNING, 1);
    }

    // refused requests
    public void incrementDeniedRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_REFUSED, 1);
    }

    // error requests
    public void incrementErrorRequests(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    public void decrementErrorRequests(String sessionOwner, String session) {
        decrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REQUESTS_ERRORS, 1);
    }

    // ----------- FILES -----------
    // stored files
    public void incrementStoredFiles(String sessionOwner, String session, int nbProducts) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.STORED_FILES, nbProducts);
    }

    // referenced files
    public void incrementReferencedFiles(String sessionOwner, String session) {
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.REFERENCED_FILES, 1);
    }

    // deleted files
    public void notifyDeletedFiles(String sessionOwner, String session) {
        // increment the number of files deleted
        incrementCount(sessionOwner, session, SessionNotifierPropertyEnum.DELETED_FILES, 1);
        // decrement the number of files stored
        decrementCount(sessionOwner, session, SessionNotifierPropertyEnum.STORED_FILES, 1);
    }

    // ----------- UTILS -----------

    // GENERIC METHODS TO BUILD NOTIFICATIONS

    // INC
    public void incrementCount(String source, String session, SessionNotifierPropertyEnum property,
            int nbProducts) {
        if (source != null && session != null) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                                 new StepPropertyInfo(StepTypeEnum.REFERENCING, property.getState(),
                                                                      property.getName(), String.valueOf(nbProducts),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionNotificationClient.increment(step);
        }
    }

    // DEC
    public void decrementCount(String source, String session, SessionNotifierPropertyEnum property,
            int nbProducts) {
        if (source != null && session != null) {
            StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session,
                                                 new StepPropertyInfo(StepTypeEnum.REFERENCING, property.getState(),
                                                                      property.getName(), String.valueOf(nbProducts),
                                                                      property.isInputRelated(),
                                                                      property.isOutputRelated()));
            sessionNotificationClient.decrement(step);
        }
    }
}