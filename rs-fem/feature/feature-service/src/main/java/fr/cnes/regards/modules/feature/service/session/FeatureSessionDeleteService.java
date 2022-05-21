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
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@MultitenantTransactional
public class FeatureSessionDeleteService implements ISessionDeleteService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSessionDeleteService.class);

    @Autowired
    private FeatureDeleteService featureDeleteService;

    @Override
    public void deleteSession(String source, String session) {
        LOGGER.info("Event received to schedule the deletion of all features of session {} and source {}",
                    session,
                    source);
        featureDeleteService.scheduleDeletion(source, Optional.of(session));
    }

}
