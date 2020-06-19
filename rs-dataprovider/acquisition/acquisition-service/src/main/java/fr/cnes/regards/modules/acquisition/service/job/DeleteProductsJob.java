/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service.job;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.AbstractJob;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterInvalidException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobParameterMissingException;
import fr.cnes.regards.framework.modules.jobs.domain.exception.JobRuntimeException;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.service.IAcquisitionProcessingService;
import fr.cnes.regards.modules.acquisition.service.IProductService;

/**
 *
 * Job to schedule deletion of {@link Product} and {@link AcquisitionProcessingChain}
 *
 * @author Sébastien Binda
 *
 */
public class DeleteProductsJob extends AbstractJob<Void> {

    private Long chainId = null;

    public static final String CHAIN_ID_PARAM = "chainId";

    private String sessionName = null;

    public static final String SESSION_NAME_PARAM = "sessionName";

    public static final String DELETE_CHAIN_PARAM = "deleteChain";

    private Boolean deleteChain = false;

    @Autowired
    private IProductService productService;

    @Autowired
    private IAcquisitionProcessingService acqProcService;

    public static Set<JobParameter> getParameters(Long chainId, Optional<String> session, boolean deleteChain) {
        Set<JobParameter> parameters = Sets.newHashSet();
        parameters.add(new JobParameter(DeleteProductsJob.CHAIN_ID_PARAM, chainId));
        if (session.isPresent()) {
            parameters.add(new JobParameter(DeleteProductsJob.SESSION_NAME_PARAM, session.get()));
        }
        parameters.add(new JobParameter(DeleteProductsJob.DELETE_CHAIN_PARAM, deleteChain));
        return parameters;
    }

    @Override
    public void setParameters(Map<String, JobParameter> parameters)
            throws JobParameterMissingException, JobParameterInvalidException {
        chainId = getValue(parameters, CHAIN_ID_PARAM);
        sessionName = (String) getOptionalValue(parameters, SESSION_NAME_PARAM).orElse(null);
        deleteChain = getValue(parameters, DELETE_CHAIN_PARAM);
        if (deleteChain == null) {
            deleteChain = Boolean.FALSE;
        }
    }

    @Override
    public void run() {
        try {
            AcquisitionProcessingChain chain = acqProcService.getChain(chainId);
            if (deleteChain) {
                acqProcService.deleteChain(chain.getId());
            } else if (sessionName != null) {
                productService.deleteBySession(chain, sessionName);
            } else {
                productService.deleteByProcessingChain(chain);
            }
        } catch (ModuleException e) {
            logger.error("Business error", e);
            throw new JobRuntimeException(e);
        }
    }
}
