/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.test.integration.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import fr.cnes.regards.test.integration.TestApplication;

/**
 * This annotation is used to pass integration tests at API level.
 * 
 * @author msordi
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.MOCK)
public @interface RegardsIntegrationTest {

}
