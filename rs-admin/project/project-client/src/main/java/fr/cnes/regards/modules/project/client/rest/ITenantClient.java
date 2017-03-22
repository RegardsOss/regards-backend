/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client.rest;

import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;

/**
 *
 * Tenant client
 * 
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/tenants", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ITenantClient {

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Set<String>> getAllTenants();

    @RequestMapping(method = RequestMethod.GET, value = "/{pMicroserviceName}")
    public ResponseEntity<Set<String>> getAllActiveTenants(@PathVariable("pMicroserviceName") String pMicroserviceName);
}
