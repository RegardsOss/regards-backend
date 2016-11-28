/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.cloud.gateway.authentication.plugins.impl.kerberos;

/**
 *
 * Class KerberosSPParameters
 *
 * PArameters Labels for Kerberos Service Provider Plugin
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public final class KerberosSPParameters {

    /**
     * Principal parameter label
     */
    public static final String PRINCIPAL_PARAMETER = "principal";

    /**
     * Realm parameter label
     */
    public static final String REALM_PARAMETER = "realm";

    /**
     * LDAP Address label
     */
    public static final String LDAP_ADRESS_PARAMETER = "ldapAdress";

    /**
     * LDAP Port parameter label
     */
    public static final String LDAP_PORT_PARAMETER = "ldapPort";

    /**
     * Krb5.conf file parameter label
     */
    public static final String KRB5_FILEPATH_PARAMETER = "krb5FilePath";

    /**
     * keytab file parameter label
     */
    public static final String KEYTAB_FILEPATH_PARAMETER = "keytabFilePath";

    /**
     * LDAP DN label
     */
    public static final String PARAM_LDAP_CN = "ldapCN";

    /**
     * LDAP Search filter parameter label
     */
    public static final String PARAM_LDAP_USER_FILTER_ATTTRIBUTE = "ldapSearchUserFilter";

    /**
     * LDAP Email attribute to retrieve
     */
    public static final String PARAM_LDAP_USER_LOGIN_ATTTRIBUTE = "ldapUserLoginAttribute";

    /**
     * LDAP Email attribute to retrieve
     */
    public static final String PARAM_LDAP_EMAIL_ATTTRIBUTE = "ldapEmail";

    private KerberosSPParameters() {
    }

}
