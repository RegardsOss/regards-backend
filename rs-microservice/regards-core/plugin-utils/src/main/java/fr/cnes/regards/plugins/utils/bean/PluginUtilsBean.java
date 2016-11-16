/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.plugins.utils.bean;

import java.lang.reflect.Field;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.plugins.utils.PluginUtilsException;
import fr.cnes.regards.plugins.utils.ReflectionUtils;

/**
 * @author Christophe Mertz
 *
 */
@Component
public class PluginUtilsBean implements IPluginUtilsBean, BeanFactoryAware {

    /**
     * Spring bean factory
     */
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(final BeanFactory pBeanFactory) {
        beanFactory = pBeanFactory;
    }

    @Override
    public <T> void processAutowiredBean(final T pPluginInstance) throws PluginUtilsException {
        // Look for annotated fields
        for (final Field field : pPluginInstance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Autowired.class)) {
                ReflectionUtils.makeAccessible(field);
                @SuppressWarnings("unchecked")
                final T effectiveVal = (T) beanFactory.getBean(field.getType());
                ReflectionUtils.makeAccessible(field);
                try {
                    field.set(pPluginInstance, effectiveVal);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new PluginUtilsException(e.getMessage());
                }
            }
        }
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

}
