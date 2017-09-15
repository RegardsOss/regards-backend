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
package fr.cnes.regards.framework.oais.builder;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * Information package builder
 *
 * @author Marc Sordi
 *
 */
public abstract class IPBuilder<T extends AbstractInformationPackage> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IPBuilder.class);

    private final T ip;

    public IPBuilder(Class<T> clazz, EntityType type) throws ReflectiveOperationException {
        try {
            ip = clazz.newInstance();
            ip.setType(type);
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Cannot instanciate information package");
            throw e;
        }
    }

    public T build() {
        return ip;
    }

    public void addTags(String... tags) {
        if (tags != null) {
            ip.getTags().addAll(Arrays.asList(tags));
        }
    }

    public void addInformationObjects(InformationObject... informationObjects) {
        if (informationObjects != null) {
            ip.getInformationObjects().addAll(Arrays.asList(informationObjects));
        }
    }
}
