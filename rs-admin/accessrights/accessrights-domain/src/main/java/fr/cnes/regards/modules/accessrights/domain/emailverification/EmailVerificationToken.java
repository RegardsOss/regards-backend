/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.domain.emailverification;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.modules.accessrights.domain.projects.ProjectUser;

/**
 * Verification token for verifying the user's email process.
 *
 * @author Xavier-Alexandre Brochard
 * @author Christophe Mertz
 *
 * @see <a>http://www.baeldung.com/registration-verify-user-by-email</a>
 */
@Entity
@Table(name = "t_email_verification_token",
        uniqueConstraints = @UniqueConstraint(name = "uk_email_verification_token_account_id",
                columnNames = { "project_user_id" }))
public class EmailVerificationToken {

    /**
     * Expiration delay in minutes (=24 hours)
     */
    private static final int EXPIRATION = 60 * 24;

    /**
     * Id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Randomly generated string
     */
    @Column(name = "token", length = 255)
    private String token;

    /**
     * The link back to the {@link ProjectUser}
     */
    @OneToOne(optional = false)
    @JoinColumn(updatable = false, name = "project_user_id",
            foreignKey = @ForeignKey(name = "fk_email_verification_token"))
    private ProjectUser projectUser;

    /**
     * The origin url
     */
    @NotBlank
    @Column(name = "origin_url", length = 255)
    private String originUrl;

    /**
     * The request link
     */
    @NotBlank
    @Column(name = "request_link", length = 255)
    private String requestLink;

    /**
     * The computed expiration date based on EXPIRATION delay in minutes
     */
    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    /**
     * Verified?
     */
    @Column(name = "verified")
    private boolean verified;

    /**
     * Default constructor
     */
    public EmailVerificationToken() {
        super();
    }

    /**
     *
     * @param pProjectUser
     *            The link back to the {@link ProjectUser}
     * @param pOriginUrl
     *            The origin url
     * @param pRequestLink
     *            The request link
     */
    public EmailVerificationToken(final ProjectUser pProjectUser, final String pOriginUrl, final String pRequestLink) {
        super();
        token = UUID.randomUUID().toString();
        projectUser = pProjectUser;
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
        expiryDate = calculateExpiryDate(EXPIRATION);
        verified = false;
    }

    /**
     * Calculate expiration date
     *
     * @param pExpiryTimeInMinutes
     *            the expiration time in minutes
     * @return the expiration date
     */
    private LocalDateTime calculateExpiryDate(final long pExpiryTimeInMinutes) {
        return LocalDateTime.now().plusMinutes(pExpiryTimeInMinutes);
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param pId
     *            the id to set
     */
    public void setId(final Long pId) {
        id = pId;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @param pToken
     *            the token to set
     */
    public void setToken(final String pToken) {
        token = pToken;
    }

    /**
     * @return the expiryDate
     */
    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    /**
     * @param pExpiryDate
     *            the expiryDate to set
     */
    public void setExpiryDate(final LocalDateTime pExpiryDate) {
        expiryDate = pExpiryDate;
    }

    /**
     * @return the verified
     */
    public boolean isVerified() {
        return verified;
    }

    /**
     * @param pVerified
     *            the verified to set
     */
    public void setVerified(final boolean pVerified) {
        verified = pVerified;
    }

    /**
     * @return the originUrl
     */
    public String getOriginUrl() {
        return originUrl;
    }

    /**
     * @return the projectUser
     */
    public ProjectUser getProjectUser() {
        return projectUser;
    }

    /**
     * @param pProjectUser the projectUser to set
     */
    public void setProjectUser(ProjectUser pProjectUser) {
        projectUser = pProjectUser;
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
