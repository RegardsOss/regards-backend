/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
import fr.cnes.regards.modules.feature.domain.FeatureEntity;
import fr.cnes.regards.modules.feature.domain.ILightFeatureEntity;
import fr.cnes.regards.modules.feature.domain.request.FeatureCreationRequest;
import fr.cnes.regards.modules.feature.domain.request.ILightFeatureCreationRequest;
import org.springframework.stereotype.Service;

@Service
@MultitenantTransactional
public class FeatureSessionNotifier {

    private static final String GLOBAL_SESSION_STEP = "feature";

    private final ISessionAgentClient sessionNotificationClient;

    public FeatureSessionNotifier(ISessionAgentClient sessionNotificationClient) {
        this.sessionNotificationClient = sessionNotificationClient;
    }

    public void incrementCount(FeatureEntity featureEntity, FeatureSessionProperty property) {
        if (featureEntity != null) {
            incrementCount(featureEntity.getSessionOwner(), featureEntity.getSession(), property);
        }
    }

    public void incrementCount(ILightFeatureEntity featureEntity, FeatureSessionProperty property) {
        if (featureEntity != null) {
            incrementCount(featureEntity.getSessionOwner(), featureEntity.getSession(), property);
        }
    }

    public void incrementCount(ILightFeatureCreationRequest request, FeatureSessionProperty property) {
        incrementCount(request.getMetadata().getSessionOwner(), request.getMetadata().getSession(), property);
    }

    public void incrementCount(FeatureCreationRequest request, FeatureSessionProperty property) {
        incrementCount(request.getMetadata().getSessionOwner(), request.getMetadata().getSession(), property);
    }

    public void incrementCount(String source, String session, FeatureSessionProperty property) {
        incrementCount(source, session, property, 1L);
    }

    public void incrementCount(String source, String session, FeatureSessionProperty property, long nbProducts) {
        StepPropertyInfo stepPropertyInfo = new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                 property.getState(),
                                                                 property.getName(),
                                                                 String.valueOf(nbProducts),
                                                                 property.isInputRelated(),
                                                                 property.isOutputRelated());
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session, stepPropertyInfo);
        sessionNotificationClient.increment(step);
    }

    public void decrementCount(FeatureEntity featureEntity, FeatureSessionProperty property) {
        if (featureEntity != null) {
            decrementCount(featureEntity.getSessionOwner(), featureEntity.getSession(), property);
        }
    }

    public void decrementCount(ILightFeatureEntity featureEntity, FeatureSessionProperty property) {
        if (featureEntity != null) {
            decrementCount(featureEntity.getSessionOwner(), featureEntity.getSession(), property);
        }
    }

    public void decrementCount(ILightFeatureCreationRequest request, FeatureSessionProperty property) {
        decrementCount(request.getMetadata().getSessionOwner(), request.getMetadata().getSession(), property);
    }

    public void decrementCount(FeatureCreationRequest request, FeatureSessionProperty property) {
        decrementCount(request.getMetadata().getSessionOwner(), request.getMetadata().getSession(), property);
    }

    public void decrementCount(String source, String session, FeatureSessionProperty property) {
        decrementCount(source, session, property, 1L);
    }

    public void decrementCount(String source, String session, FeatureSessionProperty property, long nbProducts) {

        StepPropertyInfo stepPropertyInfo = new StepPropertyInfo(StepTypeEnum.REFERENCING,
                                                                 property.getState(),
                                                                 property.getName(),
                                                                 String.valueOf(nbProducts),
                                                                 property.isInputRelated(),
                                                                 property.isOutputRelated());
        StepProperty step = new StepProperty(GLOBAL_SESSION_STEP, source, session, stepPropertyInfo);
        sessionNotificationClient.decrement(step);
    }

}