/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.stub;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;

/**
 *
 * Test IAuthoritiesProvider stub
 *
 * @author Sébastien Binda
 *
 */
@Component
public class AuthoritiesProviderStub implements IAuthoritiesProvider {

    @Override
    public List<ResourceMapping> getResourcesAccessConfiguration() {
        return new ArrayList<>();
    }

    @Override
    public List<String> getRoleAuthorizedAddress(String pRole) {
        return new ArrayList<>();
    }

    @Override
    public boolean hasCorsRequestsAccess(String pAuthority) {
        return false;
    }

}
