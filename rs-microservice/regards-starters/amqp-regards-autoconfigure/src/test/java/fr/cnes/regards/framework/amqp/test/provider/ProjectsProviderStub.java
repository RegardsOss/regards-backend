/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.amqp.test.provider;

import java.util.HashSet;
import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.multitenant.autoconfigure.tenant.ITenantResolver;

/**
 * @author svissier
 *
 */
@Component
@Primary
public class ProjectsProviderStub implements ITenantResolver {

    @Override
    public Set<String> getAllTenants() {
        final Set<String> projectListStub = new HashSet<>();
        projectListStub.add("PROJECT1");
        projectListStub.add("PROJECT2");
        return projectListStub;
    }

}
