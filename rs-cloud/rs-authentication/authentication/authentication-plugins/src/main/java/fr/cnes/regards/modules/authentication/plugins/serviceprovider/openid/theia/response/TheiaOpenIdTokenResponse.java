package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class TheiaOpenIdTokenResponse {

    @SerializedName("token_type")
    private final String tokenType;

    @SerializedName("expires_in")
    private final Long expiresIn;

    @SerializedName("refresh_token")
    private final String refreshToken;

    @SerializedName("access_token")
    private final String accessToken;

    public TheiaOpenIdTokenResponse(String tokenType, Long expiresIn, String refreshToken, String accessToken) {
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TheiaOpenIdTokenResponse that = (TheiaOpenIdTokenResponse) o;
        return Objects.equals(tokenType, that.tokenType)
            && Objects.equals(expiresIn, that.expiresIn)
            && Objects.equals(refreshToken, that.refreshToken)
            && Objects.equals(accessToken, that.accessToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, expiresIn, refreshToken, accessToken);
    }
}
