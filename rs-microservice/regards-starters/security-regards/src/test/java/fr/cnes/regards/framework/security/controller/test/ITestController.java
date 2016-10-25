/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.controller.test;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 *
 * Class ITestController
 *
 * Test controller interface
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
@RequestMapping("/tests")
public interface ITestController {

    @RequestMapping(method = RequestMethod.GET, value = "/endpoint")
    public void testMethod();

}
