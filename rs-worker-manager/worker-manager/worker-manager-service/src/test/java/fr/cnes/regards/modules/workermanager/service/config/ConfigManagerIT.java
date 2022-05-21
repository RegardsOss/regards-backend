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
import fr.cnes.regards.framework.module.manager.ModuleConfiguration;
import fr.cnes.regards.framework.module.manager.ModuleConfigurationItem;
import fr.cnes.regards.modules.workermanager.dto.WorkerConfigDto;
import fr.cnes.regards.modules.workermanager.service.cache.AbstractWorkerManagerServiceUtilsIT;
import org.assertj.core.util.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

/**
 * @author Léo Mieulet
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=worker_conf_manager" })
public class ConfigManagerIT extends AbstractWorkerManagerServiceUtilsIT {

    @Autowired
    private ConfigManager configManager;

    @Test
    public void test() {
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
                                                                                                     contentTypes1)))),
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
}
