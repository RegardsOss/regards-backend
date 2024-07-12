package fr.cnes.regards.framework.modules.plugins.annotations;

import java.lang.annotation.*;

/**
 * This annotation can be used to destroy a plugin. It must be used on a no-arg method. The method is called when
 * plugin is destroyed
 * Note the method is called without any tenant in the context
 *
 * @author oroussel
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PluginDestroy {

}
