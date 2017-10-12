package fr.cnes.regards.modules.storage.plugin.security;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 * Plugin interface for all security delegation plugins
 * @author Sylvain VISSIERE-GUERINET
 */
@PluginInterface(description = "Contract to respect by any Security Delecation plugin")
public interface ISecurityDelegation {

    /**
     * Allows to know if the current user has access to a given AIP, threw its ipId
     * @param ipId identifier of aip that we are wondeering if we have access to.
     * @return weither we have access to the aip or not
     */
    boolean hasAccess(String ipId) throws EntityNotFoundException;

    /**
     * @return weither the current user has access to features returning collections of AIP
     */
    boolean hasAccessToListFeature();
}
