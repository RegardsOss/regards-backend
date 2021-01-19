/* Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config.reactive;

import fr.cnes.regards.framework.security.endpoint.voter.ResourceAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstanceAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IInstancePublicAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.IProjectAdminAccessVoter;
import fr.cnes.regards.framework.security.utils.endpoint.ISystemAccessVoter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.vote.AffirmativeBased;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is the AccessDecisionManager config for reactive application.
 * @author gandrieu
 */
@Configuration
@ConditionalOnProperty(name = "spring.main.web-application-type", havingValue = "reactive")

public class ProcessingAccessDecisionConfiguration {

    @Autowired
    private ResourceAccessVoter resourceAccessVoter;

    @Autowired(required = false)
    private IInstanceAdminAccessVoter instanceAccessVoter;

    @Autowired(required = false)
    private IInstancePublicAccessVoter instancePublicAccessVoter;

    @Autowired(required = false)
    private ISystemAccessVoter systemAccessVoter;

    @Autowired(required = false)
    private IProjectAdminAccessVoter adminAccessVoter;

    @Bean
    protected AccessDecisionManager accessDecisionManager() {
        final List<AccessDecisionVoter<?>> decisionVoters = new ArrayList<>();

        // Manage system voter
        if (systemAccessVoter != null) {
            decisionVoters.add(systemAccessVoter);
        }

        // Manage instance voter
        if (instanceAccessVoter != null) {
            decisionVoters.add(instanceAccessVoter);
        }

        if (instancePublicAccessVoter != null) {
            decisionVoters.add(instancePublicAccessVoter);
        }

        // Manage project admin voter
        if (adminAccessVoter != null) {
            decisionVoters.add(adminAccessVoter);
        }

        decisionVoters.add(resourceAccessVoter);

        // Access granted if one of the two voter return access granted
        return new AffirmativeBased(decisionVoters);
    }

}
