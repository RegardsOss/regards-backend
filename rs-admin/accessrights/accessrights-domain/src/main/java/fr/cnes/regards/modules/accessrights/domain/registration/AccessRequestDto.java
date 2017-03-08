package fr.cnes.regards.modules.accessrights.domain.registration;

import java.util.List;

import fr.cnes.regards.modules.accessrights.domain.projects.MetaData;

/**
 * Dto class wrapping data required for both account and project user creation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class AccessRequestDto {

    /**
     * The email
     */
    private String email;

    /**
     * The first name
     */
    private String firstName;

    /**
     * The last name
     */
    private String lastName;

    /**
     * The list of meta data
     */
    private List<MetaData> metaData;

    /**
     * The password
     */
    private String password;

    /**
     * The url of the request initiator client, passed from the frontend
     */
    private String originUrl;

    /**
     * The url to redirect after clicking the activation link in the email
     */
    private String requestLink;

    /**
     * @param pEmail
     * @param pFirstName
     * @param pLastName
     * @param pMetaData
     * @param pPassword
     * @param pOriginUrl
     * @param pRequestLink
     */
    public AccessRequestDto(final String pEmail, final String pFirstName, final String pLastName,
            final List<MetaData> pMetaData, final String pPassword, final String pOriginUrl,
            final String pRequestLink) {
        super();
        email = pEmail;
        firstName = pFirstName;
        lastName = pLastName;
        metaData = pMetaData;
        password = pPassword;
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param pEmail
     *            the email to set
     */
    public void setEmail(final String pEmail) {
        email = pEmail;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param pFirstName
     *            the firstName to set
     */
    public void setFirstName(final String pFirstName) {
        firstName = pFirstName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param pLastName
     *            the lastName to set
     */
    public void setLastName(final String pLastName) {
        lastName = pLastName;
    }

    /**
     * @return the metaData
     */
    public List<MetaData> getMetaData() {
        return metaData;
    }

    /**
     * @param pMetaData
     *            the metaData to set
     */
    public void setMetaData(final List<MetaData> pMetaData) {
        metaData = pMetaData;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param pPassword
     *            the password to set
     */
    public void setPassword(final String pPassword) {
        password = pPassword;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @param pOriginUrl
     *            the originUrl to set
     */
    public void setOriginUrl(final String pOriginUrl) {
        originUrl = pOriginUrl;
    }

    /**
     * @return the requestLink
     */
    public String getRequestLink() {
        return requestLink;
    }

    /**
     * @param pRequestLink
     *            the requestLink to set
     */
    public void setRequestLink(final String pRequestLink) {
        requestLink = pRequestLink;
    }

}
