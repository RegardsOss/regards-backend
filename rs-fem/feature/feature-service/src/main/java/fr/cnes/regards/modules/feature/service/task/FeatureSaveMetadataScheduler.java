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

package fr.cnes.regards.modules.feature.service.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.modules.dump.service.scheduler.AbstractDumpScheduler;
import fr.cnes.regards.modules.feature.service.dump.FeatureSaveMetadataService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;

/**
 * Scheduler to handle feature dumps
 * @author Iliana Ghazali
 */
@Profile("!noscheduler")
@Component
public class FeatureSaveMetadataScheduler extends AbstractDumpScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureSaveMetadataScheduler.class);

    @Autowired
    private FeatureSaveMetadataService featureSaveMetadataService;

    @Override
    protected String getLockName() {
        return "scheduleFSaveMetadata";
    }

    @Override
    protected Task getDumpTask() {
        return () -> {
            LockAssert.assertLocked();
            featureSaveMetadataService.scheduleJobs();
        };
    }

    @Override
    protected String getNotificationTitle() {
        return "FEATURE SAVE METADATA SCHEDULING";
    }

    @Override
    protected String getType() {
        return "FEATURE SAVE METADATA REQUESTS";
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
