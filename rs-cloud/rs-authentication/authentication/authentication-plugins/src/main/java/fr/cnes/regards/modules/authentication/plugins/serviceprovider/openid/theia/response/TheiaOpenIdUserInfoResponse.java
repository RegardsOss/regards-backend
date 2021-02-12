package fr.cnes.regards.modules.authentication.plugins.serviceprovider.openid.theia.response;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public class TheiaOpenIdUserInfoResponse {
    @SerializedName("http://theia.org/claims/emailaddress")
    private final String email;

    @SerializedName("http://theia.org/claims/givenname")
    private final String firstname;

    @SerializedName("http://theia.org/claims/lastname")
    private final String lastname;

    @SerializedName("http://theia.org/claims/organization")
    private final String organization;

    @SerializedName("http://theia.org/claims/function")
    private final String function;

    @SerializedName("http://theia.org/claims/type")
    private final String type;

    @SerializedName("http://theia.org/claims/telephone")
    private final String telephone;

    @SerializedName("http://theia.org/claims/streetaddress")
    private final String streetAddress;

    @SerializedName("http://theia.org/claims/source")
    private final String source;

    @SerializedName("http://theia.org/claims/country")
    private final String country;

    @SerializedName("http://theia.org/claims/ignkey")
    private final String ignKey;

    @SerializedName("http://theia.org/claims/ignauthentication")
    private final String ignAuthentication;

    @SerializedName("http://theia.org/claims/role")
    private final String role;

    @SerializedName("http://theia.org/claims/regDate")
    private final String regDate;

    public TheiaOpenIdUserInfoResponse(
        String email,
        String firstname,
        String lastname,
        String organization,
        String function,
        String type,
        String telephone,
        String streetAddress,
        String source,
        String country,
        String ignKey,
        String ignAuthentication,
        String role,
        String regDate
    ) {
        this.email = email;
        this.firstname = firstname;
        this.lastname = lastname;
        this.organization = organization;
        this.function = function;
        this.type = type;
        this.telephone = telephone;
        this.streetAddress = streetAddress;
        this.source = source;
        this.country = country;
        this.ignKey = ignKey;
        this.ignAuthentication = ignAuthentication;
        this.role = role;
        this.regDate = regDate;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public String getOrganization() {
        return organization;
    }

    public String getFunction() {
        return function;
    }

    public String getType() {
        return type;
    }

    public String getTelephone() {
        return telephone;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public String getSource() {
        return source;
    }

    public String getCountry() {
        return country;
    }

    public String getIgnKey() {
        return ignKey;
    }

    public String getIgnAuthentication() {
        return ignAuthentication;
    }

    public String getRole() {
        return role;
    }

    public String getRegDate() {
        return regDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TheiaOpenIdUserInfoResponse that = (TheiaOpenIdUserInfoResponse) o;
        return Objects.equals(email, that.email)
            && Objects.equals(firstname, that.firstname)
            && Objects.equals(lastname, that.lastname)
            && Objects.equals(organization, that.organization)
            && Objects.equals(function, that.function)
            && Objects.equals(type, that.type)
            && Objects.equals(telephone, that.telephone)
            && Objects.equals(streetAddress, that.streetAddress)
            && Objects.equals(source, that.source)
            && Objects.equals(country, that.country)
            && Objects.equals(ignKey, that.ignKey)
            && Objects.equals(ignAuthentication, that.ignAuthentication)
            && Objects.equals(role, that.role)
            && Objects.equals(regDate, that.regDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, firstname, lastname, organization, function, type, telephone, streetAddress, source, country, ignKey, ignAuthentication, role, regDate);
    }
}
