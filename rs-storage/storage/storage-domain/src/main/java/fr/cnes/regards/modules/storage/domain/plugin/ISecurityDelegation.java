package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.Set;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

/**
 * Plugin interface for all security delegation plugins
 * @author Sylvain VISSIERE-GUERINET
 */
@PluginInterface(description = "Contract to respect by any security delegation plugin",
        allowMultipleConfigurationActive = false)
public interface ISecurityDelegation {

    /**
     * Return URNs of which access is granted from given ones
     */
    Set<UniformResourceName> hasAccess(Collection<UniformResourceName> urns);

    /**
     * Allow to know if the current user has access to a given AIP, through its ipId
     * @param ipId identifier of aip that we are wondeering if we have access to.
     * @return weither we have access to the aip or not
     */
    boolean hasAccess(String ipId);

    /**
     * @return weither the current user has access to features returning collections of AIP
     */
    boolean hasAccessToListFeature();
}
