package fr.cnes.regards.microservices.backend.controlers;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.microservices.backend.pojo.AccessRights;
import fr.cnes.regards.microservices.backend.pojo.Plugin;
import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;

@RestController
// Indicates that those resources are securised. Only the /oauth endpoint do not
// need the authentication token
@EnableResourceServer
@RequestMapping("/api")
public class AdminController {
	
	@Autowired
	MethodAutorizationService authService_;
	
	/**
	 * Method to iniate REST resources authorizations.
	 */
	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/api/access/rights@POST",new RoleAuthority("PUBLIC"),new RoleAuthority("USER"),new RoleAuthority("ADMIN"));
	}

	@ResourceAccess
	@RequestMapping(value = "/access/rights", method = RequestMethod.POST)
	public @ResponseBody List<AccessRights> getAccessRights(@RequestBody List<AccessRights> pAccessRights, OAuth2Authentication pPrincipal) {
		
		for (AccessRights access : pAccessRights){
			List<GrantedAuthority> auths = authService_.getAutoritiesById(access.getEndpoint()+"@"+access.getVerb());
			if (auths != null){
				boolean apiAccess = false;
				for (GrantedAuthority role : pPrincipal.getAuthorities()){
					for (GrantedAuthority auth : auths){
						if (auth.getAuthority().equals(role.getAuthority())){
							apiAccess = true;
							break;
						}
					}
					if (apiAccess == true){
						break;
					}
				}
				if (apiAccess == true){
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

}