package fr.cnes.regards.modules.accessrights.instance.domain.accountunlock;

import org.hibernate.validator.constraints.NotBlank;

/**
 * Dto class wrapping data required for the {@link AccountsController#requestAccountUnlock} endpoint.
 *
 * @author Xavier-Alexandre Brochard
 */
public class RequestAccountUnlockDto {

    /**
     * The origin url
     */
    @NotBlank
    private String originUrl;

    /**
     * The request link
     */
    @NotBlank
    private String requestLink;

    /**
     * @param pOriginUrl
     * @param pRequestLink
     */
    public RequestAccountUnlockDto(final String pOriginUrl, final String pRequestLink) {
        super();
        originUrl = pOriginUrl;
        requestLink = pRequestLink;
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
