/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
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
