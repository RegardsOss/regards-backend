/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.authentication.plugins.impl.kerberos;

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
