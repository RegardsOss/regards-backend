/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.stub;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

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
    public boolean hasCorsRequestsAccess(final String pAuthority) {
        return false;
    }

    @Override
    public List<ResourceMapping> registerEndpoints(final List<ResourceMapping> pLocalEndpoints) {
        return new ArrayList<>();
    }

    @Override
    public List<RoleAuthority> getRoleAuthorities() {
        return new ArrayList<>();
    }

}
