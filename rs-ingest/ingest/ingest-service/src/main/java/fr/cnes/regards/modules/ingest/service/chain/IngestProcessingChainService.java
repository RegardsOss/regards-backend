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
package fr.cnes.regards.modules.ingest.service.chain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.gson.GsonBuilderFactory;
import fr.cnes.regards.framework.gson.strategy.FieldNamePatternExclusionStrategy;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.ingest.dao.IIngestProcessingChainRepository;
import fr.cnes.regards.modules.ingest.dao.IngestProcessingChainSpecifications;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.service.chain.plugin.DefaultSingleAIPGeneration;
import fr.cnes.regards.modules.ingest.service.chain.plugin.DefaultSipValidation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Ingest processing service
 *
 * @author Marc Sordi
 * @author Sébastien Binda
 */
@Service
@MultitenantTransactional
public class IngestProcessingChainService implements IIngestProcessingChainService, InitializingBean {

    public static final String DEFAULT_VALIDATION_PLUGIN_CONF_LABEL = "DefaultSIPValidation";

    public static final String DEFAULT_GENERATION_PLUGIN_CONF_LABEL = "DefaultAIPGeneration";

    private static final Logger LOGGER = LoggerFactory.getLogger(IngestProcessingChainService.class);

    @Autowired
    private IIngestProcessingChainRepository ingestChainRepository;

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private GsonBuilderFactory gsonBuilderFactory;

    private Gson gsonWithIdExclusionStrategy;

    @Autowired
    private Validator validator;

    @SuppressWarnings("unused")
    @Autowired
    private IPublisher publisher;

    @Override
    public void afterPropertiesSet() {
        // Initialize specific GSON instance
        GsonBuilder customBuilder = gsonBuilderFactory.newBuilder();
        customBuilder.addSerializationExclusionStrategy(new FieldNamePatternExclusionStrategy("id"));
        gsonWithIdExclusionStrategy = customBuilder.create();
    }

    @Override
    public void initDefaultServiceConfiguration() throws ModuleException {

        LOGGER.debug("Trying to initialize default ingest chain {}", IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);

        // Check if the default IngestProcessingChain is defined
        if (!ingestChainRepository.findOneByName(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL).isPresent()) {

            LOGGER.debug("Initializing default ingest chain {}", IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);
            // Create the default chain
            IngestProcessingChain defaultChain = new IngestProcessingChain();
            defaultChain.setName(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL);

            // Create default validation plugin configuration
            PluginConfiguration validationDefaultConf = new PluginConfiguration(DEFAULT_VALIDATION_PLUGIN_CONF_LABEL,
                                                                                DefaultSipValidation.class.getAnnotation(
                                                                                    Plugin.class).id());
            defaultChain.setValidationPlugin(validationDefaultConf);

            // Create default generation plugin configuration
            PluginConfiguration generationDefaultConf = new PluginConfiguration(DEFAULT_GENERATION_PLUGIN_CONF_LABEL,
                                                                                DefaultSingleAIPGeneration.class.getAnnotation(
                                                                                    Plugin.class).id());
            defaultChain.setGenerationPlugin(generationDefaultConf);

            createNewChain(defaultChain);
        }
    }

    @Override
    public IngestProcessingChain createNewChain(IngestProcessingChain newChain) throws ModuleException {

        // Check no identifier
        if (newChain.getId() != null) {
            throw new EntityInvalidException(String.format("New chain %s must not already have and identifier.",
                                                           newChain.getName()));
        }

        // Check not already exists
        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(newChain.getName());
        if (oChain.isPresent()) {
            throw new EntityAlreadyExistsException(String.format("%s for name %s aleady exists",
                                                                 IngestProcessingChain.class.getName(),
                                                                 newChain.getName()));
        }

        // Register plugin configurations
        if (newChain.getPreProcessingPlugin().isPresent()) {
            createPluginConfiguration(newChain.getPreProcessingPlugin().get());
        }
        createPluginConfiguration(newChain.getValidationPlugin());
        createPluginConfiguration(newChain.getGenerationPlugin());
        if (newChain.getTagPlugin().isPresent()) {
            createPluginConfiguration(newChain.getTagPlugin().get());
        }
        if (newChain.getAipStorageMetadataPlugin().isPresent()) {
            createPluginConfiguration(newChain.getAipStorageMetadataPlugin().get());
        }
        if (newChain.getPostProcessingPlugin().isPresent()) {
            createPluginConfiguration(newChain.getPostProcessingPlugin().get());
        }

        // Save new chain
        return ingestChainRepository.save(newChain);
    }

    @Override
    public IngestProcessingChain createNewChain(InputStream input) throws ModuleException {
        Reader json = new InputStreamReader(input, Charset.forName("UTF-8"));
        try {
            IngestProcessingChain ipc = gsonWithIdExclusionStrategy.fromJson(json, IngestProcessingChain.class);
            // Validate entry because not already done
            Errors errors = new MapBindingResult(new HashMap<>(), "ingestProcessingChain");
            validator.validate(ipc, errors);
            if (errors.hasErrors()) {
                List<String> messages = new ArrayList<>();
                ErrorTranslator.getErrors(errors).forEach(error -> {
                    messages.add(error.toString());
                    LOGGER.error("IngestProcessingChain import error : {}", error.toString());
                });
                throw new EntityInvalidException(messages);
            }
            return createNewChain(ipc);
        } catch (JsonIOException e) {
            LOGGER.error("Cannot read JSON file containing ingestion processing chain", e);
            throw new EntityInvalidException(e.getMessage(), e);
        }
    }

    @Override
    public void exportProcessingChain(String name, OutputStream os) throws ModuleException, IOException {
        IngestProcessingChain ipc = getChain(name);
        try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(os, "UTF-8"))) {
            writer.setIndent("  ");
            gsonWithIdExclusionStrategy.toJson(ipc, IngestProcessingChain.class, writer);
        }
    }

    private PluginConfiguration createPluginConfiguration(PluginConfiguration pluginConfiguration)
        throws ModuleException {
        // Check no identifier. For each new chain, we force plugin configuration creation. A configuration cannot be
        // reused.
        if (pluginConfiguration.getId() != null) {
            throw new EntityInvalidException(String.format(
                "Plugin configuration %s must not already have and identifier.",
                pluginConfiguration.getLabel()));
        }
        return pluginService.savePluginConfiguration(pluginConfiguration);
    }

    @Override
    public IngestProcessingChain updateChain(IngestProcessingChain chainToUpdate) throws ModuleException {

        // Check already exists
        if (!ingestChainRepository.exists(chainToUpdate.getName())) {
            throw new EntityNotFoundException(chainToUpdate.getName(), IngestProcessingChain.class);
        }

        List<Optional<PluginConfiguration>> confsToRemove = new ArrayList<>();

        // Manage plugin configuration
        // ---------------------------
        // Pre-processing plugine
        Optional<PluginConfiguration> existing = ingestChainRepository.findOnePreProcessingPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(chainToUpdate.getPreProcessingPlugin(), existing));
        // Validation plugin
        existing = ingestChainRepository.findOneValidationPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(Optional.of(chainToUpdate.getValidationPlugin()), existing));
        // Generation plugin
        existing = ingestChainRepository.findOneGenerationPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(Optional.of(chainToUpdate.getGenerationPlugin()), existing));
        // Tag plugin
        existing = ingestChainRepository.findOneTagPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(chainToUpdate.getTagPlugin(), existing));
        // Aip Storage Metadata plugin
        existing = ingestChainRepository.findOneAIPStorageMetadataPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(chainToUpdate.getAipStorageMetadataPlugin(), existing));
        // Post-processing plugin
        existing = ingestChainRepository.findOnePostProcessingPluginByName(chainToUpdate.getName());
        confsToRemove.add(updatePluginConfiguration(chainToUpdate.getPostProcessingPlugin(), existing));

        // Update chain
        ingestChainRepository.save(chainToUpdate);

        // Clean unused plugin configuration after chain update avoiding foreign keys constraints restrictions.
        for (Optional<PluginConfiguration> confToRemove : confsToRemove) {
            if (confToRemove.isPresent()) {
                pluginService.deletePluginConfiguration(confToRemove.get().getBusinessId());
            }
        }

        return chainToUpdate;
    }

    /**
     * Create or update a plugin configuration cleaning old one if necessary
     *
     * @param pluginConfiguration new plugin configuration or update
     * @param existing            existing plugin configuration
     * @return configuration to remove because it is no longer used
     * @throws ModuleException if error occurs!
     */
    private Optional<PluginConfiguration> updatePluginConfiguration(Optional<PluginConfiguration> pluginConfiguration,
                                                                    Optional<PluginConfiguration> existing)
        throws ModuleException {

        Optional<PluginConfiguration> confToRemove = Optional.empty();

        if (pluginConfiguration.isPresent()) {
            PluginConfiguration conf = pluginConfiguration.get();
            if (conf.getId() == null) {
                // Delete previous configuration if exists
                confToRemove = existing;
                // Save new configuration
                pluginService.savePluginConfiguration(conf);
            } else {
                // Update configuration
                pluginService.updatePluginConfiguration(conf);
            }
        } else {
            // Delete previous configuration if exists
            confToRemove = existing;
        }

        return confToRemove;
    }

    @Override
    public void deleteChain(String name) throws ModuleException {

        Optional<IngestProcessingChain> oChain = ingestChainRepository.findOneByName(name);
        if (!oChain.isPresent()) {
            throw new EntityNotFoundException(name, IngestProcessingChain.class);
        }

        IngestProcessingChain chain = oChain.get();

        // Get related plugin configurations
        List<PluginConfiguration> plugins = chain.getChainPlugins();
        // Delete chain
        ingestChainRepository.delete(chain);
        // Delete related plugin configurations
        for (PluginConfiguration pluginConf : plugins) {
            pluginService.deletePluginConfiguration(pluginConf.getBusinessId());
        }
    }

    @Override
    public Page<IngestProcessingChain> searchChains(String name, Pageable pageable) {
        return ingestChainRepository.loadAll(IngestProcessingChainSpecifications.search(name), pageable);
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

    @Override
    public List<IngestProcessingChain> findAll() {
        return ingestChainRepository.findAll();
    }

}
