package fr.cnes.regards.framework.microservice.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.module.ready.IModuleReady;
import fr.cnes.regards.framework.module.ready.ModuleReadiness;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(MicroserviceReadyController.BASE_PATH)
public class MicroserviceReadyController {

    /**
     * Controller base path
     */
    public static final String BASE_PATH = "/ready";

    /**
     * List of all {@link IModuleReady} instantiated
     */
    @Autowired(required = false)
    private List<IModuleReady<?>> moduleReadies;

    /**
     * @return whether the microservice is ready or not with the reasons
     */
    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "allows to known if the microservice is ready to work")
    public ResponseEntity<ModuleReadiness<?>> isReady() {
        ModuleReadiness<Object> microserviceReadiness = new ModuleReadiness<Object>(Boolean.TRUE, Lists.newArrayList(),
                null);
        if ((moduleReadies != null) && !moduleReadies.isEmpty()) {
            for (IModuleReady<?> moduleReady : moduleReadies) {
                ModuleReadiness<?> moduleReadiness = moduleReady.isReady();
                microserviceReadiness.setReady(microserviceReadiness.isReady() && moduleReadiness.isReady());
                microserviceReadiness.setSpecifications(moduleReadiness.getSpecifications());
                microserviceReadiness.getReasons().addAll(moduleReadiness.getReasons());

            }
        }
        return new ResponseEntity<ModuleReadiness<?>>(microserviceReadiness, HttpStatus.OK);
    }
}
