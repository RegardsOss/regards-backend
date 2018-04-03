/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.service.plugins;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;

/**
 * Default {@link ISecurityDelegation} implementation using rs-catalog to check access rights
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Plugin handling the security thanks to catalog",
        id = "CatalogSecurityDelegation", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class CatalogSecurityDelegationTestPlugin implements ISecurityDelegation {

    @Override
    public boolean hasAccess(String ipId) throws EntityNotFoundException {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
