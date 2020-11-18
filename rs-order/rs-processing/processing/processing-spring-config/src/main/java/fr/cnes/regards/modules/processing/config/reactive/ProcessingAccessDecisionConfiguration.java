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
