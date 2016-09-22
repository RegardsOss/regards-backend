package fr.cnes.regards.microservices.backend.controllers.datamanagement;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "connection controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")@RequestMapping("/api")
public class ConnectionController {

    @Autowired MethodAutorizationService authService_;

//    /**
//     * Method to initiate REST resources authorizations.
//     */
//    @PostConstruct public void initAuthorisations() {
//        authService_.setAutorities("/api/connection@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/connection@POST", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/connection/{connection_id}@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/connection/{connection_id}@POST", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/connection/{connection_id}@PUT", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//        authService_.setAutorities("/api/connection/{connection_id}@DELETE", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"), new RoleAuthority("ADMIN"));
//    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection", method = RequestMethod.GET)
    public @ResponseBody List<Object> getConnections() {
        return null;
    }


    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection", method = RequestMethod.POST)
    public @ResponseBody List<Object> addConnection(@RequestBody List<Object> things) {
        return null;
    }


    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection/{connection_id}", method = RequestMethod.GET)
    public @ResponseBody Object getConnection(@PathVariable("connection_id") String connectionId) {
        return null;
    }


    // Todo: change this design otherwise we need to save a connection before testing it
    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection/{connection_id}", method = RequestMethod.POST)
    public @ResponseBody Object testConnection(@PathVariable("connection_id") String connectionId) {
        return null;
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection/{connection_id}", method = RequestMethod.PUT)
    public @ResponseBody Object updateConnection(@PathVariable("connection_id") String connectionId) {
        return null;
    }

    @ResourceAccess(description = "")
    @RequestMapping(value = "/connection/{connection_id}", method = RequestMethod.DELETE)
    public @ResponseBody Object removeConnection(@PathVariable("connection_id") String connectionId) {
        return null;
    }
}
