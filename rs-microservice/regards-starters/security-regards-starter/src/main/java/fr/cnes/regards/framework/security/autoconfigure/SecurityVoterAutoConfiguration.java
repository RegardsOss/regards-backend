/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.security.autoconfigure;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.framework.security.endpoint.voter.InstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.InstancePublicAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.ProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.ResourceAccessVoter;
import fr.cnes.regards.framework.security.endpoint.voter.SystemAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstancePublicAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;

/**
 * This class autoconfigures required voters based on configuration.
 * @author Marc Sordi
 */
@Configuration
@ConditionalOnWebApplication
public class SecurityVoterAutoConfiguration {

    /**
     * Global method authorization service
     */
    @Autowired
    private MethodAuthorizationService methodAuthService;

    @Value("${regards.instance.tenant.name:instance}")
    private String instanceTenantName;

    /**
     * Give full access for internal system call between microservices
     * @return {@link ISystemAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "system.voter.enabled", havingValue = "true",
            matchIfMissing = true)
    @Bean
    public ISystemAccessVoter systemAccessVoter() {
        return new SystemAccessVoter();
    }

    /**
     * Give full access for instance admin call
     * @return {@link IInstanceAdminAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "instance.voter.enabled", havingValue = "true",
            matchIfMissing = true)
    @Bean
    public IInstanceAdminAccessVoter instanceAccessVoter() {
        return new InstanceAdminAccessVoter();
    }

    /**
     * Give access to public endpoints for instance public call
     * @return {@link IInstancePublicAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "instance.voter.enabled", havingValue = "true",
            matchIfMissing = true)
    @Bean
    public IInstancePublicAccessVoter instancePublicAccessVoter() {
        return new InstancePublicAccessVoter(instanceTenantName);
    }

    /**
     * Give full access for project admin call
     * @return {@link IInstanceAdminAccessVoter}
     */
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "regards.security", name = "project.admin.voter.enabled", havingValue = "true",
            matchIfMissing = true)
    @Bean
    public IProjectAdminAccessVoter adminAccessVoter() {
        return new ProjectAdminAccessVoter();
    }

    /**
     * Manage dynamic endpoint security based on roles
     * @return {@link ResourceAccessVoter}
     */
    @ConditionalOnMissingBean
    @Bean
    public ResourceAccessVoter resourceAccessVoter() {
        return new ResourceAccessVoter(methodAuthService);
    }
}
