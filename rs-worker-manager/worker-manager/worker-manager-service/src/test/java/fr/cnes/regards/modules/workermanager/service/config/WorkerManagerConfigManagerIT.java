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

        configManager.resetConfiguration();
        Assert.assertEquals("Should delete any conf",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    @Test
    @Purpose("Verify if workflows are imported from valid workflowDtos.")
    public void import_workflow_conf_nominal() throws FileNotFoundException {
        // GIVEN
        // save corresponding worker configurations first
        WorkerConfig workerConfig1 = WorkerConfig.build("worker1", Set.of("contentType1"), "contentType2");
        WorkerConfig workerConfig2 = WorkerConfig.build("worker2", Set.of("contentType2"), "contentType3");
        workerConfigRepository.saveAll(List.of(workerConfig1, workerConfig2));

        // WHEN
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         List.of(ModuleConfigurationItem.build(
                                                                                                     getWorkflowDto(
                                                                                                         "workflow1_conf_nominal.json")),
                                                                                                 ModuleConfigurationItem.build(
                                                                                                     getWorkflowDto(
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
    public void import_workflow_conf_error() throws FileNotFoundException {
        // GIVEN
        // invalid workflows
        WorkflowConfigDto workflowConfigDtoNotExistingWorker = getWorkflowDto("workflow2_conf_nominal.json");
        WorkflowConfigDto workflowConfigDtoLongType = getWorkflowDto("workflow_conf_error_too_long_type.json");

        // WHEN
        Set<String> errors = configManager.importConfiguration(ModuleConfiguration.build(null,
                                                                                         List.of(ModuleConfigurationItem.build(
                                                                                                     workflowConfigDtoNotExistingWorker),
                                                                                                 ModuleConfigurationItem.build(
                                                                                                     workflowConfigDtoLongType))),
                                                               new HashSet<>());

        // THEN
        LOGGER.error("Errors detected during import_workflow_conf_error test :\n {}", errors);
        Assert.assertEquals("Should get errors", 2, errors.size());
        Assert.assertEquals("expected limited to 128 characters to occur once",
                            1,
                            errors.stream().filter(error -> error.contains("limited to 128 characters")).count());
        Assert.assertEquals("expected NotExistingWorkerError to occur once",
                            1,
                            errors.stream().filter(error -> error.contains("NotExistingWorkerError")).count());
        Assert.assertEquals("Should get no configuration when export",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
        Assert.assertEquals("Should get no configuration when export",
                            0,
                            configManager.exportConfiguration(Lists.newArrayList()).getConfiguration().size());
    }

    private WorkflowConfigDto getWorkflowDto(String workflow) throws FileNotFoundException {
        return gson.fromJson(new FileReader(SRC_TEST_RESOURCES_CONFIG + workflow), WorkflowConfigDto.class);
    }

}
