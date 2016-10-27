/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.domain.ResourceMapping;
import fr.cnes.regards.framework.security.domain.ResourceMappingException;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class ResourceAccessVoterTest
 *
 * Resources Access voter test class
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class MethodAuthorizationTest {

    /**
     * endpoint label
     */
    private static final String ENDPOINT = "endpoint";

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test(expected = ResourceMappingException.class)
    public void missingRessourceAccessAnnotationWithValueSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @RequestMapping(value = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        MethodAuthorizationUtils.buildResourceMapping(method);
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test(expected = ResourceMappingException.class)
    public void missingRessourceAccessAnnotationWithPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @RequestMapping(path = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        MethodAuthorizationUtils.buildResourceMapping(method);
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test(expected = ResourceMappingException.class)
    public void missingRessourceAccessAnnotationWithSameValueAndPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @RequestMapping(value = "/method_level_mapping", path = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        MethodAuthorizationUtils.buildResourceMapping(method);
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test(expected = ResourceMappingException.class)
    public void missingRessourceAccessAnnotationWithDifferentValueAndPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @RequestMapping(value = "/method_level_mapping", path = "/other_method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        MethodAuthorizationUtils.buildResourceMapping(method);
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithGetAndValueSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @RequestMapping(value = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@GET");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithPutAndValueSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @RequestMapping(value = "/method_level_mapping", method = RequestMethod.PUT)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@PUT");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @RequestMapping(path = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@GET");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithSameValueAndPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @RequestMapping(value = "/method_level_mapping", path = "/method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@GET");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test(expected = AnnotationConfigurationException.class)
    public void presentRessourceAccessAnnotationWithDifferentValueAndPathSpecified()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @RequestMapping(value = "/method_level_mapping", path = "/different_method_level_mapping", method = RequestMethod.GET)
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        MethodAuthorizationUtils.buildResourceMapping(method);
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithGETAnnotation()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @GetMapping(value = "/method_level_mapping")
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals("class_level_mapping/method_level_mapping@GET", result.getResourceMappingId());
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithPUTAnnotation()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @PutMapping(value = "/method_level_mapping")
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@PUT");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithPOSTAnnotation()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @PostMapping(value = "/method_level_mapping")
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@POST");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithDELETEAnnotation()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @DeleteMapping(value = "/method_level_mapping")
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@DELETE");
    }

    /**
     *
     * Verify introspection code to get the informations about endpoint access resources
     *
     * @throws NoSuchMethodException
     *             test error
     * @throws SecurityException
     *             test error
     * @throws ResourceMappingException
     *             test error
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_SEC_200")
    @Purpose("Verify introspection code to get the informations about endpoint access resources")
    @Test
    public void presentRessourceAccessAnnotationWithPATCHAnnotation()
            throws NoSuchMethodException, SecurityException, ResourceMappingException {
        /**
         *
         * Class Controller
         *
         * Test controller
         *
         * @author CS
         * @since 1.0-SNAPSHOT
         */
        @RequestMapping("class_level_mapping")
        class Controller {

            @ResourceAccess(name = "the name", description = "the description")
            @PatchMapping(value = "/method_level_mapping")
            public Object endpoint() {
                return null;
            }
        }
        final Method method = Controller.class.getMethod(ENDPOINT);
        final ResourceMapping result = MethodAuthorizationUtils.buildResourceMapping(method);
        Assert.assertNotNull(result);
        Assert.assertEquals(result.getResourceMappingId(), "class_level_mapping/method_level_mapping@PATCH");
    }

    /**
     *
     * Class ControllerStub
     *
     * Test class STUB
     *
     * @author CS
     * @since 1.0-SNAPSHOT
     */
    @RequestMapping("TheClassLevelRequestMapping")
    class ControllerStub {

        @RequestMapping(value = "/a", method = RequestMethod.GET)
        public Object endpointA() {
            return null;
        }

        @RequestMapping(path = "/b", method = RequestMethod.GET)
        public Object endpointB() {
            return null;
        }

        @RequestMapping(value = "/c", path = "/c", method = RequestMethod.GET)
        public Object endpointC() {
            return null;
        }

        @RequestMapping(value = "/d", path = "not_/d", method = RequestMethod.GET)
        public Object endpointD() {
            return null;
        }

        @RequestMapping(value = "/e", method = RequestMethod.PUT)
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointE() {
            return null;
        }

        @RequestMapping(path = "/f", method = RequestMethod.PUT)
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointF() {
            return null;
        }

        @RequestMapping(value = "/g", path = "/g", method = RequestMethod.PUT)
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointG() {
            return null;
        }

        @RequestMapping(value = "/h", path = "not_/h", method = RequestMethod.PUT)
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointH() {
            return null;
        }

        @GetMapping(value = "/i")
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointI() {
            return null;
        }

        @PutMapping(value = "/j")
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointJ() {
            return null;
        }

        @PostMapping(value = "/k")
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointK() {
            return null;
        }

        @DeleteMapping(value = "/l")
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointL() {
            return null;
        }

        @PatchMapping(value = "/m")
        @ResourceAccess(name = "the name", description = "the description")
        public Object endpointM() {
            return null;
        }
    }
}
