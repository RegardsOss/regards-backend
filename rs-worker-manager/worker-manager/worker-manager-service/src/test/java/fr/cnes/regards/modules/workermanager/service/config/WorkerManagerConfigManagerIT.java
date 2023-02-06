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
package fr.cnes.regards.modules.workermanager.service.config;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.workermanager.dao.IWorkerConfigRepository;
import fr.cnes.regards.modules.workermanager.dao.IWorkflowRepository;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.dto.WorkflowConfigDto;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsIT;
import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_conf_manager" })
public class WorkerManagerConfigManagerIT extends AbstractWorkerManagerServiceUtilsIT {

    private static final String SRC_TEST_RESOURCES_CONFIG = "src/test/resources/config/";

    @Autowired
    private WorkerManagerConfigManager configManager;

    @Autowired
    private IWorkerConfigRepository workerConfigRepository;

    @Autowired
    private IWorkflowRepository workflowRepository;

    @Autowired
    private Gson gson;

    @After
    public void cleanAfterImport() {
        configManager.resetConfiguration();
        Assert.assertEquals("Should delete any conf",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    @Test
    public void import_worker_conf_nominal() {
        Assert.assertEquals("Should be able to export conf, 0 at first",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
        String workerType = "workerType";
        String contentType3 = "contentType3";
        String contentType2 = "contentType2";
        String contentType1 = "contentType1";
        Set<String> contentTypes1 = Sets.newHashSet(contentType1, contentType2, contentType3);
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         Lists.newArrayList(
                                                                                             ModuleConfigurationItem.build(
                                                                                                 new WorkerConfigDto(
                                                                                                     workerType,
                                                                                                     contentTypes1,
                                                                                                     null)))),
                                                               Sets.newHashSet());
        Assert.assertEquals("Should not get errors", 0, errors.size());
        Assert.assertEquals("Should now get a configuration when export",
                            1,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    @Test
    @Purpose("Verify if workflow configuration is not imported when a worker configuration contains the same type.")
    public void import_conf_error_duplicate_types() throws FileNotFoundException {
        // GIVEN
        // workflowType = workerType => not allowed!
        WorkerConfigDto workerConfig = getWorkerConfigDto("worker1_conf_nominal.json");
        WorkflowConfigDto duplicatedWorkflowTypeConfig = getWorkflowConfigDto(
            "workflow_conf_error_duplicated_types_1.json");

        // WHEN
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         List.of(ModuleConfigurationItem.build(
                                                                                                     workerConfig),
                                                                                                 ModuleConfigurationItem.build(
                                                                                                     duplicatedWorkflowTypeConfig))),
                                                               new HashSet<>());

        // THEN
        // workflow configuration should not be imported
        LOGGER.error("Errors detected during workflow_conf_error_duplicated_types_1 test :\n {}", errors);
        Assert.assertEquals("Should get errors", 1, errors.size());
        Assert.assertEquals("expected errors did not occur",
                            1,
                            errors.stream().filter(error -> error.contains("duplicate")).count());
        // only worker configuration should be imported
        Assert.assertEquals("Should get only worker configuration when export",
                            1,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    @Test
    @Purpose("Verify if workflows are imported from valid workflowDtos.")
    public void import_workflow_conf_nominal() throws FileNotFoundException {
        // GIVEN
        // save corresponding worker configurations first
        WorkerConfig workerConfig1 = WorkerConfig.build("workerType1", Set.of("contentType1"), "contentType2");
        WorkerConfig workerConfig2 = WorkerConfig.build("workerType2", Set.of("contentType2"), "contentType3");
        workerConfigRepository.saveAll(List.of(workerConfig1, workerConfig2));

        // WHEN
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         List.of(ModuleConfigurationItem.build(
                                                                                                     getWorkflowConfigDto(
                                                                                                         "workflow1_conf_nominal.json")),
                                                                                                 ModuleConfigurationItem.build(
                                                                                                     getWorkflowConfigDto(
                                                                                                         "workflow2_conf_nominal.json")))),
                                                               new HashSet<>());

        // THEN
        Assert.assertEquals("Should not get errors", 0, errors.size());
        Assert.assertEquals(2, workflowRepository.findAll().size());
        Assert.assertEquals("Should get a configuration when export",
                            4,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
        configManager.resetConfiguration();
        Assert.assertEquals("Should delete any conf",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());

    }

    @Test
    @Purpose("Verify if configuration is not imported when workflows are malformed.")
    public void import_workflow_conf_error_malformed_workflows() throws FileNotFoundException {
        // GIVEN
        WorkflowConfigDto workflowConfigDtoTooLongType = getWorkflowConfigDto("workflow_conf_error_too_long_type.json");

        // WHEN
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         List.of(ModuleConfigurationItem.build(
                                                                                             workflowConfigDtoTooLongType))),
                                                               new HashSet<>());

        // THEN
        LOGGER.error("Errors detected during import_workflow_conf_error test :\n {}", errors);
        Assert.assertEquals("Should get errors", 1, errors.size());
        Assert.assertEquals("expected limited to 128 characters to occur once",
                            1,
                            errors.stream().filter(error -> error.contains("limited to 128 characters")).count());
        Assert.assertEquals("Should get no configuration when export",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
        Assert.assertEquals("Should get no configuration when export",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    private WorkflowConfigDto getWorkflowConfigDto(String workflowConfig) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workflowConfig), WorkflowConfigDto.class);
    }

    private WorkerConfigDto getWorkerConfigDto(String workerConfig) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workerConfig), WorkerConfigDto.class);
    }

}
