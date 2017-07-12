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
package fr.cnes.regards.plugins.utils.bean;

import java.lang.reflect.Field;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.cnes.regards.plugins.utils.PluginUtilsRuntimeException;
import fr.cnes.regards.plugins.utils.ReflectionUtils;

/**
 * @author Christophe Mertz
 *
 */
@Component
public class PluginUtilsBean implements IPluginUtilsBean, BeanFactoryAware {

    /**
     * A factory for accessing to the Spring bean container.
     */
    private BeanFactory beanFactory;

    @Override
    public void setBeanFactory(final BeanFactory pBeanFactory) {
        beanFactory = pBeanFactory;
    }

    @Override
    public <T> void processAutowiredBean(final T pPluginInstance)  {
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
                    throw new PluginUtilsRuntimeException("Unable to set the field <" + field.getName() + ">.", e);
                }
            }
        }
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

}
