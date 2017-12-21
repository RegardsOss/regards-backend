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

import java.time.OffsetDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.ExecAcquisitionProcessingChain;

/**
 *
 * @author Christophe Mertz
 *
 */
public interface IExecAcquisitionProcessingChainService {

    ExecAcquisitionProcessingChain save(ExecAcquisitionProcessingChain execProcessingChain);

    void delete(ExecAcquisitionProcessingChain execProcessingChain);

    ExecAcquisitionProcessingChain findBySession(String session) throws ModuleException;

    Page<ExecAcquisitionProcessingChain> retrieveAll(Pageable pageable);

    Page<ExecAcquisitionProcessingChain> findByChainGeneration(AcquisitionProcessingChain chainGeneration,
            Pageable pageable);

    Page<ExecAcquisitionProcessingChain> findByStartDateBetween(OffsetDateTime start, OffsetDateTime stop,
            Pageable pageable);

    Page<ExecAcquisitionProcessingChain> findByStartDateAfterAndStopDateBefore(OffsetDateTime start,
            OffsetDateTime stop, Pageable pageable);

    Page<ExecAcquisitionProcessingChain> findByStartDateAfter(OffsetDateTime start, Pageable pageable);
}
