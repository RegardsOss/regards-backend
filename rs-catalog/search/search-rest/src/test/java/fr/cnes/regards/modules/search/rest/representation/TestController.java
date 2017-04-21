/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.representation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller for the purpose of this test: check that is the return body is not an AbstractEntity then it doesn't
 * take into account the representation http message converter
 *
 * @author Sylvain Vissiere-Guerinet
 */
@RestController
public class TestController {

    public static final String TEST_BODY = "hello world, it seems that it works!";

    public static final String TEST_PATH = "/test";

    @RequestMapping(path = TEST_PATH)
    @ResponseBody
    public ResponseEntity<String> test() {
        return new ResponseEntity<>(TEST_BODY, HttpStatus.OK);
    }

}
