/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;

/**
 * Scheduled actions to process new CREATED SIPS by sending bulk request to Ingest
 * @author Christophe Mertz
 */
@Component
@Profile("!disableDataProviderTask")
@EnableScheduling
public class ScheduledDataProviderTasks {

    /**
     * Class logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ScheduledDataProviderTasks.class);

    /**
     * Resolver to know all existing tenants of the current REGARDS instance.
     */
    @Autowired
    private ITenantResolver tenantResolver;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IProductBulkRequestService productBulkRequestService;

    @Scheduled(fixedRateString = "${regards.acquisition.process.new.sip.ingest.delay:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processNewSIPBulkRequest() {
        LOG.debug("Process new SIP bulk request to ingest");
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            productBulkRequestService.runBulkRequest();
            runtimeTenantResolver.clearTenant();
        }
    }
    
    @Scheduled(fixedRateString = "${regards.acquisition.process.run.chains.delay:60000}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void processRunACtiveChains() {
        LOG.debug("Process run active chains");
        for (String tenant : tenantResolver.getAllActiveTenants()) {
            runtimeTenantResolver.forceTenant(tenant);
            productBulkRequestService.runActiveChains();
            runtimeTenantResolver.clearTenant();
        }
    }

}
