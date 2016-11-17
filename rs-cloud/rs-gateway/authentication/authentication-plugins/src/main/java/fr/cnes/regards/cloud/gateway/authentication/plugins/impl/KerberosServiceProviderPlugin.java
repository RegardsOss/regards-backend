/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl;

import fr.cnes.regards.cloud.gateway.authentication.plugins.IServiceProviderPlugin;
import fr.cnes.regards.cloud.gateway.authentication.plugins.domain.ExternalAuthenticationInformations;
import fr.cnes.regards.framework.security.utils.jwt.UserDetails;
import fr.cnes.regards.modules.plugins.annotations.Plugin;

@Plugin(author = "CS-SI", description = "Kerberos Service Provider", id = "KerberosServiceProviderPlugin",
        version = "1.0")
public class KerberosServiceProviderPlugin implements IServiceProviderPlugin {

    @Override
    public boolean checkTicketValidity(final ExternalAuthenticationInformations pAuthInformations) {
        // TODO : Check ticket validity
        return true;
    }

    @Override
    public UserDetails getUserInformations(final ExternalAuthenticationInformations pAuthInformations) {
        // TODO : Get informations from LDAP. User LdapAuthenticationPlugin ?
        final UserDetails userDetails = new UserDetails();
        userDetails.setEmail(pAuthInformations.getUserName());
        userDetails.setName(pAuthInformations.getUserName());
        userDetails.setTenant(pAuthInformations.getProject());
        return userDetails;
    }

}
