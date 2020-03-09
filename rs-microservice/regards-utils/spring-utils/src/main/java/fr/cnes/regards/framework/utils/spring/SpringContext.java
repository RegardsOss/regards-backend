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
package fr.cnes.regards.framework.utils.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * Static context spring accessor
 * @author oroussel
 */
@Component
public class SpringContext implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static ApplicationContext applicationContext;

    private static boolean refreshed;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        refreshed = true;
    }

    public static boolean hasApplicationContext() {
        return (refreshed && applicationContext != null);
    }

    /**
     * Retrieves the {@code ApplicationContext} set when Spring created and initialized the holder bean. If the
     * holder has not been created (see the class documentation for details on how to wire up the holder), or if
     * the holder has not been initialized, this accessor may return {@code null}.
     * <p/>
     * As a general usage pattern, callers should wrap this method in a check for {@link #hasApplicationContext()}.
     * That ensures both that the context is set and also that it has fully initialized. Using a context which has
     * not been fully initialized can result in unexpected initialization behaviors for some beans. The most common
     * example of this behavior is receiving unproxied references to some beans, such as beans which were supposed
     * to have transactional semantics applied by AOP. By waiting for the context refresh event, the likelihood of
     * encountering such behavior is greatly reduced.
     * @return the set context, or {@code null} if the holder bean has not been initialized
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        applicationContext = context;
    }

    public static boolean isRefreshed() {
        return refreshed;
    }

    /**
     * Retrieved configured bean/value
     * @param beanName bean or name value
     * @param <T> Type of expected bean or value
     * @return guess
     */
    @SuppressWarnings("unchecked")
    public static <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    public static <T> T getBean(Class<T> clazz) {
        return (T) applicationContext.getBean(clazz);
    }

}

