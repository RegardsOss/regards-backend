package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.request;

import feign.form.FormProperty;

import java.util.Objects;

public class TheiaOpenIdTokenRequest {

    @FormProperty("grant_type")
    private String grantType = "authorization_code";

    private String code;

    @FormProperty("redirect_uri")
    private String redirectUri;

    public TheiaOpenIdTokenRequest(String code, String redirectUri) {
        this.code = code;
        this.redirectUri = redirectUri;
    }

    public String getGrantType() {
        return grantType;
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
        TheiaOpenIdTokenRequest that = (TheiaOpenIdTokenRequest) o;
        return Objects.equals(grantType, that.grantType)
            && Objects.equals(code, that.code)
            && Objects.equals(redirectUri, that.redirectUri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grantType, code, redirectUri);
    }
}
