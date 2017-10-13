package fr.cnes.regards.modules.storage.client;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.storage.plugin.security.ISecurityDelegation;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Fake security delegation plugin for tests, do not check",
        id = "FalseSecurityDelegation", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3",
        owner = "CNES", url = "https://regardsoss.github.io/")
public class FalseSecurityDelegation implements ISecurityDelegation {

    @Override
    public boolean hasAccess(String ipId) throws EntityNotFoundException {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
