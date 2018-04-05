/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.domain.plugin;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;

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
    public boolean hasAccess(String ipId) throws EntityNotFoundException {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
