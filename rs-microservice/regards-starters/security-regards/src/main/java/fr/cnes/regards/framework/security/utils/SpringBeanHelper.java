/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.utils;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.stereotype.Component;

/**
 *
 * Class SpringBeanHelper
 *
 * Static Helper to get a Spring Bean from anywhere
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@Component
public class SpringBeanHelper implements BeanFactoryAware {

    /**
     * Static instance
     */
    private static SpringBeanHelper instance;

    /**
     * Spring Bean factory
     */
    private BeanFactory beanFactory;

    public SpringBeanHelper() {
        super();
        instance = this;
    }

    /**
     *
     * Get bean factory
     *
     * @return {@link BeanFactory}
     * @since 1.0-SNAPSHOT
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public void setBeanFactory(final BeanFactory pBeanFactory) {
        beanFactory = pBeanFactory;
    }

    /**
     *
     * Get a Bean referenced in the Spring context
     *
     * @param <T>
     *            bean for class
     * @param pBeanClass
     *            Bean class to obtain
     * @return Bean
     * @since 1.0-SNAPSHOT
     */
    public static <T> T getBean(final Class<T> pBeanClass) {
        if (instance != null) {
            return instance.getBeanFactory().getBean(pBeanClass);
        } else {
            return null;
        }
    }

}
