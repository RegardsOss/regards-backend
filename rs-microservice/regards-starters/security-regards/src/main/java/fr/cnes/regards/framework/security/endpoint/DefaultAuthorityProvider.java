package fr.cnes.regards.framework.security.endpoint;

/*
 * LICENSE_PLACEHOLDER
 */
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.security.domain.ResourceMapping;

/**
 *
 * Class DefaultAuthorityProvider
 *
 * TODO description.
 *
 * @author CS
 * @since TODO
 */
public class DefaultAuthorityProvider implements IAuthoritiesProvider {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorityProvider.class);

    @Override
    public List<ResourceMapping> registerEndpoints(final List<ResourceMapping> pLocalEndpoints) {
        LOG.warn("No Authority provider defined. Default one used."
                + " The local endpoints are not register to administration service. Only the default configuration is available");
        return pLocalEndpoints;
    }

    @Override
    public List<String> getRoleAuthorizedAddress(final String pRole) {
        LOG.warn("No Authority provider defined. Default one used. Management of Role IP filter is skipped.");
        return new ArrayList<>();
    }

    @Override
    public boolean hasCorsRequestsAccess(final String pAuthority) {
        LOG.warn("No Authority provider defined. Default one used. Management of configured CORS access is skipped.");
        return true;
    }

}
