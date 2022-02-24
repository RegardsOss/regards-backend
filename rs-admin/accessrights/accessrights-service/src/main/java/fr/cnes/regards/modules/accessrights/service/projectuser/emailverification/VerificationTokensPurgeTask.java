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
package fr.cnes.regards.modules.accessrights.service.projectuser.emailverification;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.accessrights.dao.registration.IVerificationTokenRepository;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Cron task purging the expired token repository.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 */
@Service
@RegardsTransactional
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)
public class VerificationTokensPurgeTask {

    private IVerificationTokenRepository tokenRepository;

    private ITenantResolver tenantResolver;

    private IRuntimeTenantResolver runtimeTenantResolver;

    private VerificationTokensPurgeTask self;

    public VerificationTokensPurgeTask(IVerificationTokenRepository tokenRepository, ITenantResolver tenantResolver,
                                       IRuntimeTenantResolver runtimeTenantResolver, VerificationTokensPurgeTask verificationTokensPurgeTask) {
        this.tokenRepository = tokenRepository;
        this.tenantResolver = tenantResolver;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.self = verificationTokensPurgeTask;
    }


    @Scheduled(cron = "${purge.cron.expression}")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void purgeSchedule() {
        for( String tenant : tenantResolver.getAllActiveTenants()) {
            try {
                runtimeTenantResolver.forceTenant(tenant);
                self.purgeExpired();
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void purgeExpired() {
        final LocalDateTime now = LocalDateTime.now();
        tokenRepository.deleteAllExpiredSince(now);
    }
}
