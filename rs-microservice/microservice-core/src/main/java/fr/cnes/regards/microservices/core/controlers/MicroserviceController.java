package fr.cnes.regards.microservices.core.controlers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;


@RestController
@RequestMapping("/ms")
public class MicroserviceController {

	@Value("${spring.application.name}")
	String appName_;
	
	@Autowired
    private EurekaClient discoveryClient_;
	
	@Value("${my.otherproperty}")
	String name = "Default value";
    
    public String serviceUrl() {
    	// the name is the application name defined in the application.yml
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka("myconfigserver", false);
        return instance.getHomePageUrl();
    }
    public String myserviceUrl() {
    	// the name is the application name defined in the application.yml
        InstanceInfo instance = discoveryClient_.getNextServerFromEureka(appName_, false);
        return instance.getHomePageUrl();
    }
	
	@RequestMapping("/eureka/adress")
	public String adress() {
		return "Myself : " + myserviceUrl();
	}
	
	@RequestMapping("/cloud/config")
	public String getConfigValue() {
		return name;
	}
}