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
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.oais.AbstractInformationPackage;
import fr.cnes.regards.framework.oais.InformationObject;
import fr.cnes.regards.framework.oais.urn.EntityType;

/**
 * Information package builder
 *
 * @author Marc Sordi
 *
 */
public abstract class IPBuilder<T extends AbstractInformationPackage> implements IOAISBuilder<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IPBuilder.class);

    protected final T ip;

    public IPBuilder(Class<T> clazz, EntityType type) {
        Assert.notNull(clazz, "Class is required");
        Assert.notNull(type, "Entity type is required");
        try {
            ip = clazz.newInstance();
            ip.setType(type);
        } catch (InstantiationException | IllegalAccessException e) {
            String errorMessage = "Cannot instanciate information package";
            LOGGER.error(errorMessage, e);
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public T build() {
        return ip;
    }

    /**
     * Add tags to information package
     * @param tags
     */
    public void addTags(String... tags) {
        Assert.notEmpty(tags, "Tags is required. Do not call method otherwise.");
        ip.getTags().addAll(Arrays.asList(tags));
    }

    public void addTags(Collection<String> tags) {
        Assert.notNull(tags, "Tag collection cannot be null");
        addTags(tags.toArray(new String[tags.size()]));
    }

    /**
     * Add {@link InformationObject} to this information package
     * @param informationObjects
     */
    public void addInformationObjects(InformationObject... informationObjects) {
        Assert.notEmpty(informationObjects, "Information object is required. Do not call method otherwise.");
        ip.getInformationObjects().addAll(Arrays.asList(informationObjects));
    }

    public void addInformationObjects(Collection<InformationObject> informationObjects) {
        Assert.notNull(informationObjects, "Information object collection cannot be null");
        addInformationObjects(informationObjects.toArray(new InformationObject[informationObjects.size()]));
    }
}
