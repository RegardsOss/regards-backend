/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.autoconfigure.controller.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 *
 * Class TestController
 *
 * RestController test implementation.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RestController
public class TestController implements ITestController {

    @Override
    @ResourceAccess(description = "enpoint")
    public void testMethod() {

    }

    @ResourceAccess(description = "otherEndpoint")
    @RequestMapping(method = RequestMethod.POST, value = "/endpoint/post")
    public void otherTestMethod() {

    }

}
