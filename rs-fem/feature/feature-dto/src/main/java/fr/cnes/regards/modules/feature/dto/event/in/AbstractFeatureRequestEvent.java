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
package fr.cnes.regards.modules.feature.dto.event.in;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.AbstractRequestEvent;
import fr.cnes.regards.framework.amqp.event.ISubscribable;

/**
 * Shared method for create and update requests.<br/>
 * At the moment, allows to propagate {@link #REGARDS_FILE_MODE_HEADER} in a CREATE OR UPDATE request.
 */
public abstract class AbstractFeatureRequestEvent extends AbstractRequestEvent implements ISubscribable {

    public static final String REGARDS_FILE_MODE_HEADER = AmqpConstants.REGARDS_HEADER_NS + "request.file_update_mode";

    public void setFileUpdateMode(String mode) {
        getMessageProperties().getHeaders().put(REGARDS_FILE_MODE_HEADER, mode);
    }

    public String getFileUpdateMode() {
        return getMessageProperties().getHeader(REGARDS_FILE_MODE_HEADER);
    }
}
