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
@Plugin(author = "REGARDS Team", description = "Plugin handling the security thanks to catalog",
        id = "CatalogSecurityDelegation", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class TrueCatalogSecurityDelegationPlugin implements ISecurityDelegation {

    @Override
    public boolean hasAccess(String ipId) throws EntityNotFoundException {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
