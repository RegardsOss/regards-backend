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
package fr.cnes.regards.framework.modules.session.commons.service.delete;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link ISessionDeleteService} if no bean is provided
 *
 * @author Iliana Ghazali
 **/
public class DefaultSessionDeleteService implements ISessionDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSessionDeleteService.class);

    @Override
    public void deleteSession(String source, String session) {
        LOGGER.warn("Bean missing to delete session, the session {} from source {} will not be deleted", session,
                    source);
    }
}