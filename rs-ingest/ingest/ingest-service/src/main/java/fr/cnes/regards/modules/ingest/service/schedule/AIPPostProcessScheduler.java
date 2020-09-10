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


package fr.cnes.regards.modules.ingest.service.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.ingest.dao.IAIPPostProcessRequestRepository;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.AIPPostProcessRequest;
import fr.cnes.regards.modules.ingest.service.aip.AIPPostProcessService;

/**
 * Scheduler to handle created {@link AIPPostProcessRequest}
 *
 * @author Iliana Ghazali
 */
@Profile("!noschedule")
@Component
public class AIPPostProcessScheduler {
    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAIPPostProcessRequestRepository repo;


    @Autowired
    private AIPPostProcessService aipPostProcessService;

    /**
     * Bulk save queued items every second.
     */
    @Scheduled(fixedDelayString = "${regards.aips.postprocess.bulk.delay:10000}", initialDelay = 1_000)
    protected void scheduleAIPPostProcessingJobs() {
        //TODO what the fuck? create all job of one tenant and only then get to the next tenant?
        // it is better not to do the do-while and decrease delay or invert for and do-while place
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                boolean stop = false;
                do {
                    stop = aipPostProcessService.scheduleJob() == null;
                } while (!stop);
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }
}


