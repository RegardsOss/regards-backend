/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.hateoas.autoconfigure;

import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;

/**
 * @author msordi
 *
 */
public class Test {

    public void test() {

        EntityLinks links = null;
        // ControllerLinkBuilder.linkTo(controller)
        // LinkBuilder builder = links.linkFor(CustomerResource.class);
        Link link = links.linkToSingleResource(Test.class, 1L);

        // Test.class.getMethod("test", parameterTypes)

        ControllerLinkBuilder.methodOn(Test.class).test();
    }
}
