package fr.cnes.regards.microservices.core.auth;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("VOID")
public @interface ResourceAccess {

	/**
	 * 
	 * @return resource name
	 */
	String name();
	
	/**
	 * 
	 * @return http method
	 */
	String method();
}
