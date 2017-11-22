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
package fr.cnes.regards.modules.ingest.service.chain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.authentication.IAuthenticationResolver;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.domain.JobInfo;
import fr.cnes.regards.framework.modules.jobs.domain.JobParameter;
import fr.cnes.regards.framework.modules.jobs.service.IJobInfoService;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.IngestProcessingChainSpecifications;
import fr.cnes.regards.modules.ingest.domain.builder.AIPEntityBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.AIPState;
import fr.cnes.regards.modules.ingest.domain.entity.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipGeneration;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultSipValidation;
import fr.cnes.regards.modules.storage.domain.AIP;

/**
 * Ingest processing service
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class IngestProcessingService implements IIngestProcessingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingService.class);

    public static final String DEFAULT_INGEST_CHAIN_LABEL = "DefaultProcessingChain";

    public static final String DEFAULT_VALIDATION_PLUGIN_CONF_LABEL = "DefaultSIPValidation";

    public static final String DEFAULT_GENERATION_PLUGIN_CONF_LABEL = "DefaultAIPGeneration";

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Autowired
    private ISIPService sipService;

    @Autowired
    private IJobInfoService jobInfoService;

    @Autowired
    private IAuthenticationResolver authResolver;

    @Autowired
    private IPluginService pluginService;

    @PostConstruct
    public void initDefaultPluginPackages() {
        pluginService.addPluginPackage(IAipGeneration.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultSingleAIPGeneration.class.getPackage().getName());
        pluginService.addPluginPackage(DefaultSipValidation.class.getPackage().getName());
    }

    @Override
    public void initDefaultServiceConfiguration() throws ModuleException {

        // Check if the default IngestProcessingChain is defined
        if (!ingestChainRepository.findOneByName(DEFAULT_INGEST_CHAIN_LABEL).isPresent()) {
            // Create the default chain
            IngestProcessingChain defaultChain = new IngestProcessingChain();
            defaultChain.setName(DEFAULT_INGEST_CHAIN_LABEL);
            PluginConfiguration validationDefaultConf = pluginService.savePluginConfiguration(new PluginConfiguration(
                    PluginUtils.createPluginMetaData(DefaultSipValidation.class),
                    DEFAULT_VALIDATION_PLUGIN_CONF_LABEL));
            PluginConfiguration generationDefaultConf = pluginService.savePluginConfiguration(new PluginConfiguration(
                    PluginUtils.createPluginMetaData(DefaultSingleAIPGeneration.class),
                    DEFAULT_GENERATION_PLUGIN_CONF_LABEL));

            // Check if default plugin configurations are defined
            Optional<PluginConfiguration> oConf = pluginService
                    .findPluginConfigurationByLabel(DEFAULT_VALIDATION_PLUGIN_CONF_LABEL);
            if (oConf.isPresent()) {
                validationDefaultConf = oConf.get();
            }
            oConf = pluginService.findPluginConfigurationByLabel(DEFAULT_GENERATION_PLUGIN_CONF_LABEL);
            if (oConf.isPresent()) {
                generationDefaultConf = oConf.get();
            }

            defaultChain.setValidationPlugin(validationDefaultConf);
            defaultChain.setGenerationPlugin(generationDefaultConf);
            this.createNewChain(defaultChain);
        }

    }

    @Override
    public void ingest() {
        // Retrieve all created sips
        // In order to avoid loading all rawSip in memory (can be huge), retrieve only the needed id and processing
        // parameters of SIPEntity
        List<Object[]> sips = sipRepository.findIdAndProcessingByState(SIPState.CREATED);
        sips.forEach(sip -> this.scheduleIngestProcessingJob((Long) sip[0], (String) sip[1]));
    }

    @Override
    public SIPEntity updateSIPEntityState(Long pId, SIPState pNewState) {
        SIPEntity sip = sipRepository.findOne(pId);
        sip.setState(pNewState);
        return sipService.saveSIPEntity(sip);
    }

    @Override
    public SIPEntity getSIPEntity(Long pId) {
        return sipRepository.findOne(pId);
    }

    @Override
    public AIPEntity createAIP(Long pSipEntityId, AIPState pAipState, AIP pAip) {
        SIPEntity sip = sipRepository.findOne(pSipEntityId);
        return aipRepository.save(AIPEntityBuilder.build(sip, AIPState.CREATED, pAip));
    }

    /**
     * Schedule a new {@link IngestProcessingJob} to ingest given {@link SIPEntity}
     * @param pSipToProcess {@link SIPEntity} to ingest
     */
    private void scheduleIngestProcessingJob(Long sipIdToProcess, String processingChain) {
        LOGGER.debug("Scheduling new IngestProcessingJob for SIP {} and processing chain {}", sipIdToProcess,
                     processingChain);
        Set<JobParameter> jobParameters = Sets.newHashSet();
        jobParameters.add(new JobParameter(IngestProcessingJob.SIP_PARAMETER, sipIdToProcess));
        jobParameters.add(new JobParameter(IngestProcessingJob.CHAIN_NAME_PARAMETER, processingChain));
        JobInfo jobInfo = new JobInfo(1, jobParameters, authResolver.getUser(), IngestProcessingJob.class.getName());
        jobInfoService.createAsQueued(jobInfo);
        sipRepository.updateSIPEntityState(SIPState.QUEUED, sipIdToProcess);
    }

    @Override
    public IngestProcessingChain createNewChain(IngestProcessingChain newChain) throws ModuleException {
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(newChain.getName());
        if (!oChain.isPresent()) {
            this.createOrUpdatePluginConfigurations(newChain);
            return ingestChainRepository.save(newChain);
        } else {
            throw new EntityAlreadyExistsException(String
                    .format("%s for name %s aleady exists", IngestProcessingChain.class.getName(), newChain.getName()));
        }
    }

    @Override
    public IngestProcessingChain updateChain(IngestProcessingChain chainToUpdate) throws ModuleException {
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(chainToUpdate.getName());
        if (oChain.isPresent()) {
            this.createOrUpdatePluginConfigurations(chainToUpdate);
            return ingestChainRepository.save(chainToUpdate);
        } else {
            throw new EntityNotFoundException(chainToUpdate.getName(), IngestProcessingChain.class);
        }
    }

    @Override
    public void deleteChain(String name) throws ModuleException {
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(name);
        if (oChain.isPresent()) {
            ingestChainRepository.delete(oChain.get());
        } else {
            throw new EntityNotFoundException(name, IngestProcessingChain.class);
        }
    }

    @Override
    public Page<IngestProcessingChain> searchChains(String name, Pageable pageable) {
        return ingestChainRepository.findAll(IngestProcessingChainSpecifications.search(name), pageable);
    }

    @Override
    public IngestProcessingChain getChain(String name) throws ModuleException {
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(name);
        if (oChain.isPresent()) {
            return oChain.get();
        } else {
            throw new EntityNotFoundException(name, IngestProcessingChain.class);
        }
    }

    @Override
    public boolean existsChain(String name) {
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(name);
        return oChain.isPresent();
    }

    /**
     * Creates or updates {@link PluginConfiguration} of each step of the {@link IngestProcessingChain}.
     * @param ingestChain {@link IngestProcessingChain}
     * @throws ModuleException
     */
    private void createOrUpdatePluginConfigurations(IngestProcessingChain ingestChain) throws ModuleException {
        // Save new plugins conf, and update existing ones if they changed
        if (ingestChain.getPreProcessingPlugin().isPresent()) {
            ingestChain.setPreProcessingPlugin(createOrUpdatePluginConfiguration(ingestChain.getPreProcessingPlugin()
                    .get()));
        }
        if (ingestChain.getValidationPlugin() != null) {
            ingestChain.setValidationPlugin(createOrUpdatePluginConfiguration(ingestChain.getValidationPlugin()));
        }
        if (ingestChain.getGenerationPlugin() != null) {
            ingestChain.setGenerationPlugin(createOrUpdatePluginConfiguration(ingestChain.getGenerationPlugin()));
        }
        if (ingestChain.getTagPlugin().isPresent()) {
            ingestChain.setTagPlugin(createOrUpdatePluginConfiguration(ingestChain.getTagPlugin().get()));
        }
        if (ingestChain.getPostProcessingPlugin().isPresent()) {
            ingestChain.setPostProcessingPlugin(createOrUpdatePluginConfiguration(ingestChain.getPostProcessingPlugin()
                    .get()));
        }
    }

    /**
     * Create or update the given {@link PluginConfiguration}
     * @param pluginConfiguration {@link PluginConfiguration} to save or update
     * @return saved or updated {@link PluginConfiguration}
     * @throws ModuleException
     */
    public PluginConfiguration createOrUpdatePluginConfiguration(PluginConfiguration pluginConfiguration)
            throws ModuleException {
        if (pluginConfiguration.getId() == null) {
            return pluginService.savePluginConfiguration(pluginConfiguration);
        } else {
            PluginConfiguration existingConf = pluginService.getPluginConfiguration(pluginConfiguration.getId());
            if (pluginConfiguration.compareTo(existingConf)) {
                return pluginService.savePluginConfiguration(pluginConfiguration);
            }
        }
        return pluginConfiguration;
    }
}
