/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.feign;

import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import feign.FeignException;
import fr.cnes.regards.framework.feign.security.FeignSecurityManager;

/**
 * @author Marc Sordi
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FeignClientTests.Application.class, webEnvironment = WebEnvironment.DEFINED_PORT,
        value = { "spring.application.name=feignclienttest", "server.port=30333",
                "logging.level.org.springframework.cloud.netflix.feign.valid=DEBUG", "feign.httpclient.enabled=false",
                "feign.okhttp.enabled=false", "jwt.secret=123456789" })
@Ignore
public class FeignClientTests {

    private static final String HELLO_MESSAGE = "Hello world";

    @Autowired
    private ApplicationContext context;

    @Autowired
    private IHelloClient helloClient;

    @SpringBootApplication
    @RestController
    @EnableFeignClients("unkown.package")
    protected static class Application {

        @RequestMapping(method = RequestMethod.GET, value = "/hello")
        public ResponseEntity<Hello> getHello() {
            Hello hello = new Hello();
            hello.setMessage(HELLO_MESSAGE);
            return ResponseEntity.ok(hello);
        }

        @RequestMapping(method = RequestMethod.GET, value = "/hello503")
        public ResponseEntity<Hello> getHello503() {
            return new ResponseEntity<>(HttpStatus.SERVICE_UNAVAILABLE);
        }

        public static void main(String[] args) {
            new SpringApplicationBuilder(Application.class)
                    .properties("spring.application.name=feignclienttest", "management.contextPath=/admin").run(args);
        }
    }

    public static class Hello {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String pMessage) {
            message = pMessage;
        }
    }

    /**
     * Test {@link FeignClient} are discovered properly
     */
    @Test
    public void testAnnnotations() {
        Map<String, Object> beans = this.context.getBeansWithAnnotation(FeignClient.class);
        Assert.assertTrue(beans.size() > 0);
        Assert.assertNotNull("Hello client should exist", helloClient);
    }

    @Test
    public void testHelloClient() {
        FeignSecurityManager.asSystem();
        ResponseEntity<Hello> response = helloClient.getHello();
        Hello hello = response.getBody();
        Assert.assertEquals(HELLO_MESSAGE, hello.getMessage());
    }

    @Test
    public void testHello404Client() {
        FeignSecurityManager.asSystem();
        ResponseEntity<Hello> response = helloClient.getHello404();
        Assert.assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Assert.assertNull(response.getBody());
    }

    @Test
    public void testHello503Client() {
        FeignSecurityManager.asSystem();
        try {
            helloClient.getHello503();
            Assert.fail("Feign exception should be thrown");
        } catch (FeignException e) {
            Assert.assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), e.status());
        }
    }

}
