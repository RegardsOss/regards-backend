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
package fr.cnes.regards.modules.ingest.service.schedule;

import fr.cnes.regards.framework.modules.dump.service.scheduler.AbstractDumpScheduler;
import fr.cnes.regards.modules.ingest.service.dump.AIPSaveMetadataService;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.core.LockingTaskExecutor.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static fr.cnes.regards.modules.ingest.service.schedule.SchedulerConstant.*;

/**
 * Scheduler to handle aip dumps
 *
 * @author Iliana Ghazali
 */
// should not put profile @Profile("!noscheduler") because {@link DumpSettings} need access to {@link AbstractDumpScheduler}
@Component
public class AIPSaveMetadataScheduler extends AbstractDumpScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AIPSaveMetadataScheduler.class);

    @Autowired
    private AIPSaveMetadataService aipSaveMetadataService;

    @Override
    protected String getLockName() {
        return AIP_SAVE_METADATA_REQUEST_LOCK;
    }

    @Override
    protected Task getDumpTask() {
        return () -> {
            LockAssert.assertLocked();
            aipSaveMetadataService.scheduleJobs();
        };
    }

    @Override
    protected String getNotificationTitle() {
        return AIP_SAVE_METADATA_TITLE;
    }

    @Override
    protected String getType() {
        return AIP_SAVE_METADATA_REQUESTS;
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
