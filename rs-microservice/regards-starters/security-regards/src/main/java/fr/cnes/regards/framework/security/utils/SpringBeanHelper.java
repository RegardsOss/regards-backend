/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * Class SpringBeanHelper
 *
 * Static Helper to get a Spring Bean from anywhere
 * @author Sébastien Binda
 */
@Component
public class SpringBeanHelper implements BeanFactoryAware {

    /**
     * Static instance
     */
    private static final SpringBeanHelper INSTANCE = new SpringBeanHelper();

    /**
     * Spring Bean factory
     */
    private BeanFactory beanFactory;

    /**
     * Get bean factory
     * @return {@link BeanFactory}
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    @Override
    public void setBeanFactory(final BeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Get a Bean referenced in the Spring context
     * @param <T> bean for class
     * @param beanClass Bean class to obtain
     * @return Bean
     */
    public static <T> T getBean(final Class<T> beanClass) {
        return INSTANCE.getBeanFactory().getBean(beanClass);
    }

}
