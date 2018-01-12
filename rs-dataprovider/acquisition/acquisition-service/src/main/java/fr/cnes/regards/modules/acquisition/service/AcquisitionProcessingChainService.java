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
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInconsistentIdentifierException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.acquisition.builder.ExecAcquisitionProcessingChainBuilder;
import fr.cnes.regards.modules.acquisition.dao.IAcquisitionProcessingChainRepository;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionProcessingChain2;
import fr.cnes.regards.modules.acquisition.domain.ExecAcquisitionProcessingChain;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaProduct;
import fr.cnes.regards.modules.acquisition.service.job.ProductAcquisitionJob;

/**
 * Manage global {@link AcquisitionProcessingChain2} life cycle
 *
 * @author Christophe Mertz
 *
 */
@MultitenantTransactional
@Service
public class AcquisitionProcessingChainService implements IAcquisitionProcessingChainService2 {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AcquisitionProcessingChainService.class);

    /**
     * {@link AcquisitionProcessingChain2} repository
     */
    private final IAcquisitionProcessingChainRepository processingChainRepository;

    /**
     * {@link JobInfo} service
     */
    private final IJobInfoService jobInfoService;

    /**
     * {@link MetaProduct} service
     */
    private final IMetaProductService metaProductService;

    /**
     * {@link ExecAcquisitionProcessingChain} service
     */
    private final IExecAcquisitionProcessingChainService execProcessingChainService;

    /**
     * {@link Plugin} service
     */
    private final IPluginService pluginService;

    /**
     * Resolver to retrieve authentication information
     */
    @Autowired
    private IAuthenticationResolver authResolver;

    /**
     * Constructor with the bean method's member as parameters
     * @param processingChainRepository the {@link IAcquisitionProcessingChainRepository}
     * @param execProcessingChainService the {@link IExecAcquisitionProcessingChainService}
     * @param metaProductService the {@link IMetaProductService}
     * @param jobInfoService the {@link IJobInfoService}
     * @param pluginService the {@link IPluginService}
     */
    public AcquisitionProcessingChainService(IAcquisitionProcessingChainRepository processingChainRepository,
            IExecAcquisitionProcessingChainService execProcessingChainService, IMetaProductService metaProductService,
            IJobInfoService jobInfoService, IPluginService pluginService) {
        super();
        this.processingChainRepository = processingChainRepository;
        this.execProcessingChainService = execProcessingChainService;
        this.metaProductService = metaProductService;
        this.jobInfoService = jobInfoService;
        this.pluginService = pluginService;
    }

    @Override
    public AcquisitionProcessingChain2 save(AcquisitionProcessingChain2 acqProcessingChain) {
        return processingChainRepository.save(acqProcessingChain);
    }

    @Override
    public AcquisitionProcessingChain2 update(Long chainId, AcquisitionProcessingChain2 acqProcessingChain)
            throws ModuleException {
        if (!chainId.equals(acqProcessingChain.getId())) {
            throw new EntityInconsistentIdentifierException(chainId, acqProcessingChain.getId(),
                    acqProcessingChain.getClass());
        }
        if (!processingChainRepository.exists(chainId)) {
            throw new EntityNotFoundException(chainId, AcquisitionProcessingChain2.class);
        }

        return processingChainRepository.save(createOrUpdate(acqProcessingChain));
    }

    @Override
    public AcquisitionProcessingChain2 createOrUpdate(AcquisitionProcessingChain2 acqProcessingChain)
            throws ModuleException { // NOSONAR
        if (acqProcessingChain == null) {
            return null;
        }

        createOrUpdatePluginConfigurations(acqProcessingChain);

        if (acqProcessingChain.getId() == null) {
            // It is a new Chain --> create it
            acqProcessingChain.setMetaProduct(metaProductService.createOrUpdate(acqProcessingChain.getMetaProduct()));
            return processingChainRepository.save(acqProcessingChain);
        } else {
            AcquisitionProcessingChain2 existingChain = this.retrieveComplete(acqProcessingChain.getId());
            acqProcessingChain.setMetaProduct(metaProductService.createOrUpdate(acqProcessingChain.getMetaProduct()));
            if (existingChain.equals(acqProcessingChain)) {
                // it is the same --> just return it
                return acqProcessingChain;
            } else {
                // it is different --> update it
                return processingChainRepository.save(acqProcessingChain);
            }
        }
    }

    /**
     * Creates or updates {@link PluginConfiguration} of each {@link Plugin} of the {@link AcquisitionProcessingChain2}
     * @param chain {@link AcquisitionProcessingChain2}
     * @throws ModuleException if error occurs!
     */
    private void createOrUpdatePluginConfigurations(AcquisitionProcessingChain2 chain) throws ModuleException {
        // Save new plugins conf, and update existing ones if they changed
        if (chain.getCheckAcquisitionPluginConf() != null) {
            chain.setCheckAcquisitionPluginConf(createOrUpdatePluginConfiguration(chain
                    .getCheckAcquisitionPluginConf()));
        }
        if (chain.getGenerateSipPluginConf() != null) {
            chain.setGenerateSipPluginConf(createOrUpdatePluginConfiguration(chain.getGenerateSipPluginConf()));
        }
        if (chain.getPostProcessSipPluginConf().isPresent()) {
            chain.setPostProcessSipPluginConf(createOrUpdatePluginConfiguration(chain.getPostProcessSipPluginConf()
                    .get()));
        }
    }

    /**
     * @param checkAcquisitionPluginConf
     * @return
     * @throws ModuleException
     */
    private PluginConfiguration createOrUpdatePluginConfiguration(PluginConfiguration pluginConfiguration)
            throws ModuleException {
        if (pluginConfiguration.getId() == null) {
            return pluginService.savePluginConfiguration(pluginConfiguration);
        } else {
            PluginConfiguration existingConf = pluginService.getPluginConfiguration(pluginConfiguration.getId());
            if (!pluginConfiguration.equals(existingConf)) {
                return pluginService.savePluginConfiguration(pluginConfiguration);
            }
        }
        return pluginConfiguration;
    }

    @Override
    public Page<AcquisitionProcessingChain2> retrieveAll(Pageable page) {
        return processingChainRepository.findAll(page);
    }

    @Override
    public AcquisitionProcessingChain2 retrieve(Long id) {
        return processingChainRepository.findOne(id);
    }

    @Override
    public AcquisitionProcessingChain2 retrieveComplete(Long id) {
        AcquisitionProcessingChain2 chain = this.retrieve(id);

        if (chain.getMetaProduct() != null) {
            chain.setMetaProduct(metaProductService.retrieveComplete(chain.getMetaProduct().getId()));
        }

        if (chain.getScanAcquisitionPluginConf() != null) {
            chain.setScanAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getScanAcquisitionPluginConf().getId()));
        }

        if (chain.getCheckAcquisitionPluginConf() != null) {
            chain.setCheckAcquisitionPluginConf(pluginService
                    .loadPluginConfiguration(chain.getCheckAcquisitionPluginConf().getId()));
        }

        if (chain.getGenerateSipPluginConf() != null) {
            chain.setGenerateSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getGenerateSipPluginConf().getId()));
        }

        if (chain.getPostProcessSipPluginConf().isPresent()) {
            chain.setPostProcessSipPluginConf(pluginService
                    .loadPluginConfiguration(chain.getPostProcessSipPluginConf().get().getId()));
        }

        return chain;
    }

    @Override
    public void delete(Long id) {
        processingChainRepository.delete(id);
    }

    @Override
    public void delete(AcquisitionProcessingChain2 acqProcessingChain) {
        processingChainRepository.delete(acqProcessingChain);
    }

    @Override
    public AcquisitionProcessingChain2 findByMetaProduct(MetaProduct metaProduct) {
        return processingChainRepository.findByMetaProduct(metaProduct);
    }

    @Override
    public void run(Long id) {
        run(this.retrieve(id));
    }

    @Override
    public void run(AcquisitionProcessingChain2 chain) { // NOSONAR
        // the AcquisitionProcessingChain must be active
        if (!chain.isActive()) {
            LOGGER.warn("[{}] Unable to run a not active chain generation", chain.getLabel());
            return;
        }

        // the AcquisitionProcessingChain must not be already running
        if (chain.isRunning()) {
            LOGGER.warn("[{}] Unable to run an already running chain generation", chain.getLabel());
            return;
        }

        // the difference between the previous activation date and current time must be greater than the periodicity
        if ((chain.getLastDateActivation() != null)
                && chain.getLastDateActivation().plusSeconds(chain.getPeriodicity()).isAfter(OffsetDateTime.now())) {
            LOGGER.warn("[{}] Unable to run the chain generation : the last activation date is too close from now with the periodicity {}.",
                        chain.getLabel(), chain.getPeriodicity());
            return;
        }

        // the AcquisitionProcessingChain is ready to be started
        chain.setRunning(true);
        chain.setSession(chain.getLabel() + ":" + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ":"
                + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME));
        processingChainRepository.save(chain);

        // Create the ExecAcquisitionProcessingChain
        execProcessingChainService.save(ExecAcquisitionProcessingChainBuilder.build(chain.getSession()).withChain(chain)
                .withStartDate(OffsetDateTime.now()).get());

        LOGGER.info("[{}] a new session is created : {}", chain.getLabel(), chain.getSession());

        // Create a ScanJob
        JobInfo acquisition = new JobInfo();
        acquisition.setParameters(new JobParameter(ProductAcquisitionJob.CHAIN_PARAMETER, chain));
        acquisition.setClassName(ProductAcquisitionJob.class.getName());
        acquisition.setOwner(authResolver.getUser());

        jobInfoService.createAsQueued(acquisition);
    }

    @Override
    public void runActiveChains() {
        // Retrieve all chains
        Set<AcquisitionProcessingChain2> chains = processingChainRepository.findByActiveTrueAndRunningFalse();
        chains.forEach(ch -> run(ch));
    }

}
