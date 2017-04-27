/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.maintenancetest.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;

/**
 *
 * RestController test implementation.
 *
 * @author Christophe Mertz
 * @since 1.0-SNAPSHOT
 */
@RestController
public class TestController {

    public static final String MAINTENANCE_TEST_URL = "/maintenance/test";

    public static final String MAINTENANCE_TEST_503_URL = "/maintenance/test/error";

    @RequestMapping(method = RequestMethod.GET, path = MAINTENANCE_TEST_URL)
    @ResponseBody
    public ResponseEntity<Void> testMethod() {
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = MAINTENANCE_TEST_503_URL)
    @ResponseBody
    public ResponseEntity<Void> testMethodUnavailable() {
        throw new RuntimeException("it is a runtime exception");
    }

    @RequestMapping(method = RequestMethod.POST, path = MAINTENANCE_TEST_URL)
    @ResponseBody
    public ResponseEntity<String> otherTestMethod() {
        return new ResponseEntity<>("that's created", HttpStatus.CREATED);
    }

}
