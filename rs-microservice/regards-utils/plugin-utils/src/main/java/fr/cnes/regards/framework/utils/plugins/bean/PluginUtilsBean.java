/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.utils.plugins.bean;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.stereotype.Component;

/**
 * @author Christophe Mertz
 */
@Component
public class PluginUtilsBean {

    private static AutowireCapableBeanFactory beanFactory;

    private PluginUtilsBean(AutowireCapableBeanFactory autowireCapableBeanFactory) {
        PluginUtilsBean.beanFactory = autowireCapableBeanFactory;
    }

    /**
     * Allows to autowire bean into a plugin instance
     */
    public static <T> void processAutowiredBean(final T plugin) {
        beanFactory.autowireBean(plugin);
    }

    public static boolean isBeanFactoryInitialized() {
        return beanFactory != null;
    }
}
