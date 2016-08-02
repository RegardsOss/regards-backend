package fr.cnes.regards.microservices.core.controllers;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;

import fr.cnes.regards.microservices.core.auth.MethodAutorizationService;
import fr.cnes.regards.microservices.core.auth.ResourceAccess;
import fr.cnes.regards.microservices.core.auth.RoleAuthority;


@ConditionalOnProperty(name="eureka.client.enabled",havingValue="true")
@RestController
@RequestMapping("/eureka")
public class EurekaController {
	
	/**
	 * Eureka client
	 */
	@Autowired
    private EurekaClient discoveryClient_;
	
	/**
	 * Is the Eureka server enabled
	 */
	@Value("${eureka.enabled}")
	boolean eurekaServerEnabled_ = false;

	/**
	 * Microservice application name
	 */
	@Value("${spring.application.name}")
	String appName_;
	
	/**
	 * Is the Config server enabled
	 */
	@Value("${cloud.config.server.enabled}")
	boolean configServerEnabled_ = false;
	
	/**
	 * Config server application name
	 */
	@Value("${cloud.config.server.name}")
	String configServerName_;
	
	@Autowired
	MethodAutorizationService authService_;

	@PostConstruct
	public void initAuthorisations() {
		authService_.setAutorities("/eureka/me@GET",new RoleAuthority("ADMIN"));
	}
	
    
    public String configServiceUrl() {
    	// the name is the application name defined in the application.yml
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka(configServerName_, false);
        return instance.getHomePageUrl();
    }
    public String myserviceUrl() {
    	// the name is the application name defined in the application.yml
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka(appName_, false);
        return instance.getHomePageUrl();
    }
	
    /**
     * Return the adress of the current application from the Eureka registry
     * @return
     */
    @ResourceAccess
	@RequestMapping(value="me",method=RequestMethod.GET)
	public String me() {
		if (eurekaServerEnabled_){
			return "Myself : " + myserviceUrl();
		} else {
			return "Eureka server disabled !";
		}
	}
	
	/**
     * Return the adress of the current application from the Eureka registry
     * @return
     */
    @ResourceAccess
	@RequestMapping(value="config/adress",method=RequestMethod.GET)
	public String configAddress() {
		if (eurekaServerEnabled_ && configServerEnabled_){
			return "Config server : " + configServiceUrl();
		} else {
			return "Eureka server disabled !";
		}
	}
}