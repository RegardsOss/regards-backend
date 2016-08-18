package fr.cnes.regards.microservices.backend.controllers.administration;

import fr.cnes.regards.microservices.backend.pojo.administration.AccessRights;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;

import static java.util.Arrays.asList;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

@RestController
@EnableResourceServer
@RequestMapping("/api")
public class EndpointController {

    @Autowired
    MethodAutorizationService authService_;

    /**
     * Method to initiate REST resources authorizations.
     */
    @PostConstruct
    public void initAuthorisations() {
        authService_.setAutorities("/api/endpoints@GET", new RoleAuthority("PUBLIC"), new RoleAuthority("ADMIN"));
        authService_.setAutorities("/api/access/rights@POST", new RoleAuthority("PUBLIC"), new RoleAuthority("USER"),
                new RoleAuthority("ADMIN"));
    }

    @RequestMapping(value = "/endpoints", method = RequestMethod.GET)
    public @ResponseBody ResponseEntity<HashMap<String, String>> getEndpoints() {
        return ResponseEntity.ok(buildEndpoints());
    }

    /**
     * Get the acces rights for the current logged in user for a list of depencendies (couple Verb + Endpoint)
     *
     * @param pAccessRights : AccessRights to check
     * @param pPrincipal    : Logged in user credentials
     * @return List<AccessRights>
     */
    @ResourceAccess
    @RequestMapping(value = "/access/rights", method = RequestMethod.POST)
    public @ResponseBody List<AccessRights> getAccessRights(@RequestBody List<AccessRights> pAccessRights,
                                                            OAuth2Authentication pPrincipal) {

        for (AccessRights access : pAccessRights) {
            List<GrantedAuthority> auths = authService_
                    .getAutoritiesById(access.getEndpoint() + "@" + access.getVerb());
            if (auths != null) {
                boolean apiAccess = false;
                for (GrantedAuthority role : pPrincipal.getAuthorities()) {
                    for (GrantedAuthority auth : auths) {
                        if (auth.getAuthority().equals(role.getAuthority())) {
                            apiAccess = true;
                            break;
                        }
                    }
                    if (apiAccess == true) {
                        break;
                    }
                }
                if (apiAccess == true) {
                    System.out.println("Acces granted to : " + access.getId());
                    access.setAccess(true);
                } else {
                    System.out.println("Acces denied to : " + access.getId());
                    access.setAccess(false);
                }
            } else {
                System.out.println("Acces denied to : " + access.getId());
                access.setAccess(false);
            }

        }
        return pAccessRights;
    }

    public HashMap<String, String> buildEndpoints() {
        // Note this is unfortunately hand-written. If you add a new entity, have to manually add a new link
        final List<Link> links = asList(linkTo(methodOn(ProjectController.class).getProjects()).withRel("projects_url"),
                linkTo(methodOn(ProjectAccountController.class).getProjectAccounts())
                        .withRel("projects_users_url"));

        final HashMap<String, String> map = new HashMap<>();
        for (Link link : links) {
            map.put(link.getRel(), link.getHref());
        }

        return map;
    }

}
