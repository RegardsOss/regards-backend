/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.accessrights.service.registration;

import java.util.Locale;

import org.springframework.context.ApplicationEvent;

import fr.cnes.regards.modules.accessrights.domain.instance.Account;

/**
 * Event transporting the logic needed for account email validation.
 *
 * @author Xavier-Alexandre Brochard
 */
public class OnRegistrationCompleteEvent extends ApplicationEvent {

    /**
     * Generated serial
     */
    private static final long serialVersionUID = -7099682370525387294L;

    /**
     * The app url
     */
    private final String appUrl;

    /**
     * The locale
     */
    private final Locale locale;

    /**
     * The registered account
     */
    private final Account account;

    public OnRegistrationCompleteEvent(final Account pUser, final Locale pLocale, final String pAppUrl) {
        super(pUser);

        this.account = pUser;
        this.locale = pLocale;
        this.appUrl = pAppUrl;
    }

    /**
     * @return the appUrl
     */
    public String getAppUrl() {
        return appUrl;
    }

    /**
     * @return the locale
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * @return the account
     */
    public Account getAccount() {
        return account;
    }
}