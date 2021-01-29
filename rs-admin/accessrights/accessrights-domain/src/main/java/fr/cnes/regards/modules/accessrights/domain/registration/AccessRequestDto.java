package fr.cnes.regards.modules.accessrights.domain.registration;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

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
    @Valid
    @NotBlank
    @Length(max = 128)
    private String email;

    /**
     * The first name
     */
    @Valid
    @NotBlank
    @Length(max = 128)
    private String firstName;

    /**
     * The last name
     */
    @Valid
    @NotBlank
    @Length(max = 128)
    private String lastName;

    /**
     * The role name requested
     */
    private String roleName;

    /**
     * The list of meta data
     */
    @Valid
    private List<MetaData> metadata;

    /**
     * The password
     */
    @Valid
    @NotBlank
    @Length(max = 255)
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
     * @param pEmail the email
     * @param pFirstName the first name
     * @param pLastName the last name
     * @param pRoleName the role name
     * @param pMetaData the meta data
     * @param pPassword the password
     * @param pOriginUrl necessary for frontend to redirect when the user clicks on validation link in email
     * @param pRequestLink necessary for frontend to redirect when the user clicks on validation link in email
     */
    public AccessRequestDto(final String pEmail, final String pFirstName, final String pLastName, //NOSONAR
            final String pRoleName, final List<MetaData> pMetaData, final String pPassword, final String pOriginUrl,
            final String pRequestLink) {
        super();
        email = pEmail;
        firstName = pFirstName;
        lastName = pLastName;
        metadata = pMetaData;
        password = pPassword;
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
        roleName = pRoleName;
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

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(final String pRoleName) {
        roleName = pRoleName;
    }

    /**
     * @return the metadata
     */
    public List<MetaData> getMetadata() {
        return metadata;
    }

    /**
     * @param pMetaData
     *            the metadata to set
     */
    public void setMetadata(final List<MetaData> pMetaData) {
        metadata = pMetaData;
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
