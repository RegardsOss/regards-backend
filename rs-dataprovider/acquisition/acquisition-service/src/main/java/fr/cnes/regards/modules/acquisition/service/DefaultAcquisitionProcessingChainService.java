/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;

/**
 *
 * Acquisition processing chain service
 *
 * @author Marc Sordi
 *
 */
@Service
@MultitenantTransactional
public class DefaultAcquisitionProcessingChainService implements IAcquisitionProcessingChainService {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAcquisitionProcessingChainService.class);

    @Autowired
    private IAcquisitionProcessingChainRepository processingChainRepository;
}
