/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.client.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.feign.annotation.RestClient;
import fr.cnes.regards.framework.jpa.multitenant.properties.TenantConnection;

/**
 *
 * Tenant connection client
 *
 * @author Marc Sordi
 *
 */
@RestClient(name = "rs-admin")
@RequestMapping(value = "/connections/{microservice}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
public interface ITenantConnectionClient {

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<TenantConnection> addTenantConnection(@PathVariable String microservice,
            @Valid @RequestBody TenantConnection tenantConnection);

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<List<TenantConnection>> getTenantConnections(@PathVariable String microservice);

}
