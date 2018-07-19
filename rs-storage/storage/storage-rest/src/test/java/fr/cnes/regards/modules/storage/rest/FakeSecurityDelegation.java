package fr.cnes.regards.modules.storage.rest;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.modules.storage.domain.plugin.ISecurityDelegation;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Plugin(author = "REGARDS Team", description = "Fake security delegation plugin for tests, do not check",
        id = "FakeSecurityDelegation", version = "1.0", contact = "regards@c-s.fr", licence = "GPLv3", owner = "CNES",
        url = "https://regardsoss.github.io/")
public class FakeSecurityDelegation implements ISecurityDelegation {

    @Override
    public Set<UniformResourceName> hasAccess(Collection<UniformResourceName> urns) {
        return Sets.newHashSet(urns);
    }

    @Override
    public boolean hasAccess(String ipId) {
        return true;
    }

    @Override
    public boolean hasAccessToListFeature() {
        return true;
    }
}
