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
package fr.cnes.regards.framework.plugins.autoconfigure;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Configuration;

import fr.cnes.regards.framework.amqp.autoconfigure.AmqpAutoConfiguration;
import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.MultitenantJpaAutoConfiguration;

/**
 * Class PluginUtilsAutoConfiguration A bean used to defined a implementation of {@link BeanFactoryAware}.
 * @deprecated
 * @author Christophe Mertz
 */
@Configuration
@AutoConfigureAfter({ MultitenantJpaAutoConfiguration.class, AmqpAutoConfiguration.class })
@Deprecated
// a supprimer lorsque l'on aura valider l'utilité de @AutoConfigureAfter
// utiliser plutôt plugins-service ou plugins-rest suivant le besoin
public class PluginUtilsAutoConfiguration {

}
