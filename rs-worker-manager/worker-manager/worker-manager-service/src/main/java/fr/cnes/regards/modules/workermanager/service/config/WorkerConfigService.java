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
package fr.cnes.regards.modules.workermanager.service.config;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.validation.ErrorTranslator;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.service.config.settings.WorkerManagerSettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.Validator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to handle worker configuration.<br>
 *
 * @author Léo Mieulet
 */
@Service
@MultitenantTransactional
public class WorkerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerConfigService.class);

    private final IWorkerConfigRepository workerConfigRepository;

    private final WorkerManagerSettingsService workerManagerSettingsService;

    private final Validator validator;

    public WorkerConfigService(IWorkerConfigRepository workerConfigRepository,
                               WorkerManagerSettingsService workerManagerSettingsService,
                               Validator validator) {
        this.workerConfigRepository = workerConfigRepository;
        this.workerManagerSettingsService = workerManagerSettingsService;
        this.validator = validator;
    }

    /**
     * Search for all worker configuration.
     */
    public List<WorkerConfig> searchAll() {
        return workerConfigRepository.findAll();
    }

    /**
     * Search for the configuration of a given worker type.
     *
     * @param workerType worker type
     * @return {@link WorkerConfig}
     */
    public Optional<WorkerConfig> search(String workerType) {
        return workerConfigRepository.findByWorkerType(workerType);
    }

    /**
     * Search for the configuration of a given worker types.
     *
     * @param workerTypes list of worker type
     * @return {@link WorkerConfig}
     */
    public List<WorkerConfig> search(List<String> workerTypes) {
        return workerConfigRepository.findByWorkerTypeIn(workerTypes);
    }

    /**
     * Save the worker config provided into repo
     *
     * @param workerConfig entity to update
     */
    public void update(WorkerConfig workerConfig) {
        LOGGER.info("Updating existing plugin configuration {}", workerConfig.getWorkerType());
        workerConfigRepository.save(workerConfig);
    }

    public void create(WorkerConfig workerConfig) {
        LOGGER.info("Creating worker configuration {}", workerConfig.getWorkerType());
        workerConfigRepository.save(workerConfig);
    }

    /**
     * Delete provided worker configuration
     *
     * @param workerConfig entity to delete
     */
    public void delete(WorkerConfig workerConfig) {
        workerConfigRepository.delete(workerConfig);
    }

    /**
     * Create or update WorkerConfig using provided configuration
     *
     * @param configuration a list of configuration imported
     */
    public Set<String> importConfiguration(Set<WorkerConfigDto> configuration) {
        Set<String> errors = new HashSet<>();
        for (WorkerConfigDto workerConfigDto : configuration) {
            // Check valid conf
            boolean currentWorkerConfValid = isWorkerConfValid(errors, workerConfigDto);
            if (currentWorkerConfValid) {
                Optional<WorkerConfig> workerConfigOpt = search(workerConfigDto.getWorkerType());
                if (workerConfigOpt.isPresent()) {
                    // Update entities on base
                    WorkerConfig workerConfig = workerConfigOpt.get();
                    workerConfig.setContentTypeInputs(workerConfigDto.getContentTypeInputs());
                    workerConfig.setContentTypeOutput(workerConfigDto.getContentTypeOutput());
                    workerConfig.setKeepErrors(workerConfigDto.isKeepErrors());
                    update(workerConfig);
                } else {
                    // Create missing worker config
                    create(WorkerConfig.build(workerConfigDto.getWorkerType(),
                                              workerConfigDto.getContentTypeInputs(),
                                              workerConfigDto.getContentTypeOutput(),
                                              workerConfigDto.isKeepErrors()));
                }
            }
        }
        return errors;
    }

    private boolean isWorkerConfValid(Set<String> errors, WorkerConfigDto workerConfigDto) {
        boolean currentWorkerConfValid = true;
        // check workerConfigDto model
        Errors modelViolations = new MapBindingResult(new HashMap<>(), WorkerConfigDto.class.getName());
        validator.validate(workerConfigDto, modelViolations);
        if (modelViolations.hasErrors()) {
            currentWorkerConfValid = false;
            errors.addAll(ErrorTranslator.getErrors(modelViolations));
        }

        // Check if this WorkerConfig use content type already used by another WorkerConfig(s)
        List<WorkerConfig> workerConfigUsingSameContentTypes = workerConfigRepository.findAllByContentTypeInputsIn(
            workerConfigDto.getContentTypeInputs());
        if (!workerConfigUsingSameContentTypes.isEmpty()) {
            // Get the list of worker types that conflicts with the current one
            Set<String> workerTypes = workerConfigUsingSameContentTypes.stream()
                                                                       .map(WorkerConfig::getWorkerType)
                                                                       .collect(Collectors.toSet());
            // Get the list of content types that are conflicting
            Set<String> commonContentTypes = workerConfigUsingSameContentTypes.stream()
                                                                              .map(WorkerConfig::getContentTypeInputs)
                                                                              .flatMap(Collection::stream)
                                                                              .filter(contentType -> workerConfigDto.getContentTypeInputs()
                                                                                                                    .contains(
                                                                                                                        contentType))
                                                                              .collect(Collectors.toSet());

            String errorMessage = String.format(
                "WorkerConf with type=%s declares contentType %s which are already used by %s",
                workerConfigDto.getWorkerType(),
                commonContentTypes,
                workerTypes);
            LOGGER.error(errorMessage);
            currentWorkerConfValid = false;
            errors.add(errorMessage);
        }

        // Retrieve list of content types configured to be automatically skipped
        List<String> contentTypesToSkip = workerManagerSettingsService.getValue(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME);
        List<String> contentTypesInsideSkipConf = contentTypesToSkip.stream()
                                                                    .filter(contentTypeToSkip -> workerConfigDto.getContentTypeInputs()
                                                                                                                .contains(
                                                                                                                    contentTypeToSkip))
                                                                    .toList();
        if (!contentTypesInsideSkipConf.isEmpty()) {

            String errorMessage = String.format(
                "WorkerConf with type=%s declares contentType %s which are already used inside the %s setting",
                workerConfigDto.getWorkerType(),
                contentTypesInsideSkipConf,
                WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME);
            LOGGER.error(errorMessage);
            currentWorkerConfValid = false;
            errors.add(errorMessage);
        }
        return currentWorkerConfValid;
    }
}
