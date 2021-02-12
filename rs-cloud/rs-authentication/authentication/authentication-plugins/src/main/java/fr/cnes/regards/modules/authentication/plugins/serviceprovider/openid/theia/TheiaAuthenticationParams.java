package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia;

import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;
import fr.cnes.regards.modules.authentication.domain.plugin.serviceprovider.ServiceProviderAuthenticationParams;

import java.util.Objects;

import static fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.TheiaOpenIdConnectPlugin.ID;

@GsonDiscriminator(ID)
public class TheiaAuthenticationParams extends ServiceProviderAuthenticationParams {

    private final String code;

    private final String redirectUri;

    public TheiaAuthenticationParams(String code, String redirectUri) {
        super(ID);
        this.code = code;
        this.redirectUri = redirectUri;
    }

    public String getCode() {
        return code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) return false;
        TheiaAuthenticationParams that = (TheiaAuthenticationParams) o;
        return Objects.equals(code, that.code)
            && Objects.equals(redirectUri, that.redirectUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), code, redirectUri);
    }
}
