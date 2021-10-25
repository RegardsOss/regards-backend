/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.tenant.settings.service.IDynamicTenantSettingService;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerConfig;
import fr.cnes.regards.modules.workermanager.domain.config.WorkerManagerSettings;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_config_service" })
public class WorkerConfigServiceIT extends AbstractWorkerManagerServiceUtilsTest {

    @Autowired
    private WorkerConfigService workerConfigService;

    @Autowired
    private IDynamicTenantSettingService dynamicTenantSettingService;

    @Test
    public void testImportConf() throws ModuleException {
        String workerType1 = "workerType1";
        HashSet<String> contentType1 = Sets.newHashSet("contentType1");
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentType1)));
        Optional<WorkerConfig> workerConfigOptional = workerConfigService.search(workerType1);

        Assert.assertTrue("should retrieve one config", workerConfigOptional.isPresent());
        Assert.assertEquals("should match the conf", contentType1, workerConfigOptional.get().getContentTypes());

        // test update previous workerType

        HashSet<String> contentType2 = Sets.newHashSet("contentType2");
        workerConfigService.importConfiguration(Sets.newHashSet(new WorkerConfigDto(workerType1, contentType2)));
        List<WorkerConfig> workerConfigs = workerConfigService.searchAll();
        Assert.assertEquals("still one configuration", 1, workerConfigs.size());

        workerConfigOptional = workerConfigService.search(workerType1);
        Assert.assertTrue("should retrieve one config", workerConfigOptional.isPresent());
        Assert.assertEquals("should retrieve updated conf", contentType2, workerConfigOptional.get().getContentTypes());

        workerConfigService.delete(workerConfigService.search(workerType1).get());
        Assert.assertEquals("should delete", 0, workerConfigService.searchAll().size());
    }

    @Test
    public void testInvalidConfEmptyAttribute() {
        Assert.assertEquals("should return an error when empty content type", 1,
                            workerConfigService.importConfiguration(
                                    Sets.newHashSet(new WorkerConfigDto("workerInvalid", new HashSet<>()))).size());
        Assert.assertEquals("should return an error when empty worker type", 1, workerConfigService.importConfiguration(
                Sets.newHashSet(new WorkerConfigDto("", Sets.newHashSet("contentType")))).size());
    }

    @Test
    public void testInvalidContentTypes()
            throws EntityOperationForbiddenException, EntityInvalidException, EntityNotFoundException {

        String workerType = "workerType";
        String workerType2 = "workerType2";
        String workerType3 = "workerType3";
        String contentType6 = "contentType6";
        String contentType5 = "contentType5";
        String contentType4 = "contentType4";
        String contentType3 = "contentType3";
        String contentType2 = "contentType2";
        String contentType1 = "contentType1";
        Set<String> contentTypes1 = Sets.newHashSet(contentType1, contentType2, contentType3);
        Set<String> contentTypes2 = Sets.newHashSet(contentType4, contentType5);
        // also contains the contentType1
        Set<String> contentTypes3 = Sets.newHashSet(contentType1, contentType2, contentType4, contentType6);
        Assert.assertEquals("should return an error when importing conflicting content types", 1,
                            workerConfigService.importConfiguration(
                                    Sets.newHashSet(new WorkerConfigDto(workerType, contentTypes1),
                                                    new WorkerConfigDto(workerType2, contentTypes2),
                                                    new WorkerConfigDto(workerType3, contentTypes3))).size());

        String contentType7 = "contentType6";
        String workerType4 = "workerType4";
        Set<String> contentTypes7 = Sets.newHashSet(contentType7);

        dynamicTenantSettingService.update(WorkerManagerSettings.SKIP_CONTENT_TYPES_NAME,
                                           Lists.newArrayList(contentType7));

        Assert.assertEquals("should return an error when importing conflicting content types with SKIP_CONTENT_TYPES",
                            1, workerConfigService.importConfiguration(
                        Sets.newHashSet(new WorkerConfigDto(workerType4, contentTypes7))).size());
    }
}
