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
package fr.cnes.regards.modules.feature.dto.event.in;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.modules.feature.dto.urn.FeatureUniformResourceName;

/**
 * Event received by Feature Manager to acknowledge a feature has been received by a specific recipient
 *
 * @author Léo Mieulet
 */
@Event(target = Target.ONE_PER_MICROSERVICE_TYPE, converter = JsonMessageConverter.GSON)
public class DisseminationAckEvent implements ISubscribable {

    private FeatureUniformResourceName urn;

    private String recipientLabel;

    public DisseminationAckEvent() {
    }

    public DisseminationAckEvent(FeatureUniformResourceName urn, String recipientLabel) {
        this.urn = urn;
        this.recipientLabel = recipientLabel;
    }

    public FeatureUniformResourceName getUrn() {
        return urn;
    }

    public void setUrn(FeatureUniformResourceName urn) {
        this.urn = urn;
    }

    public String getRecipientLabel() {
        return recipientLabel;
    }

    public void setRecipientLabel(String recipientLabel) {
        this.recipientLabel = recipientLabel;
    }
}