package fr.cnes.regards.microservices.core.information;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.validator.constraints.URL;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ModuleInfo {

    String name();

    String description() default "";

    String version();

    String author();

    String legalOwner();

    URL documentation();

}
