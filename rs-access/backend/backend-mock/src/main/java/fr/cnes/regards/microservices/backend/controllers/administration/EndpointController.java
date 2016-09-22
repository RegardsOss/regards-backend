/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.backend.controllers.administration;

import static java.util.Arrays.asList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.controllers.datamanagement.ConnectionController;
import fr.cnes.regards.microservices.backend.pojo.administration.AccessRights;
import fr.cnes.regards.microservices.core.annotation.ModuleInfo;
import fr.cnes.regards.microservices.core.security.endpoint.MethodAutorizationService;
import fr.cnes.regards.microservices.core.security.endpoint.ResourceMapping;
import fr.cnes.regards.microservices.core.security.endpoint.annotation.ResourceAccess;

@RestController
@ModuleInfo(name = "endpoint controller", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS", documentation = "http://test")
@RequestMapping("/api")
public class EndpointController {

    @Autowired
    MethodAutorizationService authService_;

    // /**
    // * Method to initiate REST resources authorizations.
    // */
    // @PostConstruct
    // public void initAuthorisations() {
    // authService_.setAutorities("/api/endpoints@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
    // authService_.setAutorities("/api/access/rights@POST", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"),
    // new RoleAuthority("ADMIN"));
    // }

    @RequestMapping(value = "/endpoints", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<HashMap<String, String>> getEndpoints() {
        return ResponseEntity.ok(buildEndpoints());
    }

    /**
     * Get the access rights for the current logged in user for a list of depencendies (couple Verb + Endpoint)
     *
     * @param pAccessRights
     *            : AccessRights to check
     * @param pPrincipal
     *            : Logged in user credentials
     * @return List<AccessRights>
     */
    @ResourceAccess(description = "")
    @RequestMapping(value = "/access/rights", method = RequestMethod.POST)
    public @ResponseBody List<AccessRights> getAccessRights(@RequestBody List<AccessRights> pAccessRights,
            ResourceMapping pPrincipal) {

        for (AccessRights access : pAccessRights) {
            Optional<List<GrantedAuthority>> auths = authService_.getAuthorities(pPrincipal);

            boolean apiAccess = false;

            if (auths != null && auths.isPresent()) {
                ResourceAccess resourceId = pPrincipal.getResourceAccess();
                
                // TODO : CMZ à revoir
                
//                resourceId.name()
//                for (GrantedAuthority role : pPrincipal.getResourceAccess(). getAuthorities()) {
//                    for (GrantedAuthority auth : auths.get()) {
//                        if (auth.getAuthority().equals(role.getAuthority())) {
//                            apiAccess = true;
//                            break;
//                        }
//                    }
//                    if (apiAccess) {
//                        break;
//                    }
//                }
            }

            System.out.println(apiAccess ? "Acces granted to : " + access.getId()
                    : "Acces denied to : " + access.getId());
            access.setAccess(apiAccess);

        }
        return pAccessRights;
    }

    private HashMap<String, String> buildEndpoints() {
        // Note this is unfortunately hand-written. If you add a new entity, have to manually add a new link
        final List<Link> links = asList(linkTo(methodOn(ProjectController.class).getProjects()).withRel("projects_url"),
                                        linkTo(methodOn(ProjectAccountController.class).getProjectAccounts())
                                                .withRel("projects_users_url"),
                                        linkTo(methodOn(ConnectionController.class).getConnections())
                                                .withRel("projects_connections_url"));

        final HashMap<String, String> map = new HashMap<>();
        for (Link link : links) {
            map.put(link.getRel(), link.getHref());
        }

        return map;
    }

}
