/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.service.settings;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.ingest.dao.IDumpSettingsRepository;
import fr.cnes.regards.modules.ingest.domain.settings.DumpSettings;
import fr.cnes.regards.modules.ingest.service.schedule.AIPSaveMetadataScheduler;
import fr.cnes.regards.modules.ingest.service.conf.IngestConfigurationManager;

/**
 * see {@link IDumpManagerService}
 * @author Iliana Ghazali
 */

@Service
@MultitenantTransactional
public class DumpManagerService implements IDumpManagerService {

    @Autowired
    private IDumpSettingsRepository dumpRepository;

    @Autowired
    private IRuntimeTenantResolver runTimeTenantResolver;

    @Autowired
    private AIPSaveMetadataScheduler schedulerHandler;

    @Autowired
    private IDumpSettingsService dumpSettingsService;

    @Override
    public void updateDumpAndScheduler(DumpSettings newDumpSettings) throws ModuleException {
        // PARAMETER CHECK
        // check cron expression
        String cronTrigger = newDumpSettings.getCronTrigger();
        if (!CronSequenceGenerator.isValidExpression(cronTrigger)) {
            throw new EntityInvalidException(String.format("Cron Expression %s is not valid.", cronTrigger));
        }

        // UPDATE DUMP SETTINGS if they already exist
        dumpSettingsService.update(newDumpSettings);

        // UPDATE SCHEDULER
        schedulerHandler.updateScheduler(runTimeTenantResolver.getTenant(), newDumpSettings);
    }

    @Override
    public DumpSettings getCurrentDumpSettings() {
        return dumpRepository.findFirstBy().orElse(null);
    }
}
