/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * {@link ISecurityDelegation} implementation that always return <code>true</code>.
 *
 * @author Christophe Mertz
 */
@Plugin(author = "REGARDS Team", description = "Plugin does not apply any security control",
        id = "NoCatalogSecurityDelegation", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class NoCatalogSecurityDelegationPlugin implements ISecurityDelegation {

    @Override
    public Set<UniformResourceName> hasAccess(Collection<UniformResourceName> urns) {
        return Sets.newHashSet(urns);
    }

    @Override
    public boolean hasAccess(String ipId) throws EntityNotFoundException {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
