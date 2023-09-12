package fr.cnes.regards.framework.encryption.sensitive;

import java.lang.annotation.*;

/**
 * Mark a String field as sensitive so that it can be encrypted, decrypted or masked at runtime.
 *
 * @author Iliana Ghazali
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
@Documented
public @interface StringSensitive {

}
