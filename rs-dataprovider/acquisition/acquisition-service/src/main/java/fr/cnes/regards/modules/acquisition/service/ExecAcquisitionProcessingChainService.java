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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.acquisition.dao.IExecAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.ExecAcquisitionProcessingChain;

/**
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class ExecAcquisitionProcessingChainService implements IExecAcquisitionProcessingChainService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ExecAcquisitionProcessingChainService.class);

    /**
     * {@link IExecAcquisitionProcessingChainRepository} bean
     */
    private final IExecAcquisitionProcessingChainRepository processRepository;

    /**
     * Constructor with the bean method's member as parameter
     * @param repository a {@link IExecAcquisitionProcessingChainRepository} bean
     */
    public ExecAcquisitionProcessingChainService(IExecAcquisitionProcessingChainRepository repository) {
        super();
        this.processRepository = repository;
    }

    @Override
    public ExecAcquisitionProcessingChain save(ExecAcquisitionProcessingChain execProcessingChain) {
        return processRepository.save(execProcessingChain);
    }

    @Override
    public void delete(ExecAcquisitionProcessingChain execProcessingChain) {
        processRepository.delete(execProcessingChain);
    }

    @Override
    public Page<ExecAcquisitionProcessingChain> retrieveAll(Pageable pageable) {
        return processRepository.findAll(pageable);
    }

    @Override
    public Page<ExecAcquisitionProcessingChain> findByChainGeneration(AcquisitionProcessingChain chainGeneration,
            Pageable pageable) {
        return processRepository.findByChainGeneration(chainGeneration, pageable);
    }

    @Override
    public Page<ExecAcquisitionProcessingChain> findByStartDateBetween(OffsetDateTime start, OffsetDateTime stop,
            Pageable pageable) {
        return processRepository.findByStartDateBetween(start, stop, pageable);
    }

    @Override
    public Page<ExecAcquisitionProcessingChain> findByStartDateAfterAndStopDateBefore(OffsetDateTime start,
            OffsetDateTime stop, Pageable pageable) {
        return processRepository.findByStartDateAfterAndStopDateBefore(start, stop, pageable);
    }

    @Override
    public Page<ExecAcquisitionProcessingChain> findByStartDateAfter(OffsetDateTime start, Pageable pageable) {
        return processRepository.findByStartDateAfter(start, pageable);
    }

    @Override
    public ExecAcquisitionProcessingChain findBySession(String session) throws ModuleException {
        ExecAcquisitionProcessingChain chain = processRepository.findBySession(session);
        if (chain == null) {
            String message = String.format("Unknown exec processing chain for session %s", session);
            LOGGER.error(message);
            throw new EntityNotFoundException(message);
        }
        return chain;
    }
}
