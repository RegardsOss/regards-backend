/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.configurer;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

/**
 *
 * Interface ICustomWebSecurityConfiguration
 *
 * Interface to define specific WebSecurity configurer
 *
 * @author Sébastien Binda
 *
 */
public interface ICustomWebSecurityConfiguration {

    /**
     *
     * Configure HttpSecurity
     *
     * @param pHttp
     *            HttpSecurity
     * @throws Exception
     *             configuration exception
     */
    void configure(final HttpSecurity pHttp) throws Exception;

}
