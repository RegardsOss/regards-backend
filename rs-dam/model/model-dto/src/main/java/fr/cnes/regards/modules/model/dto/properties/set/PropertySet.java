/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.model.dto.properties.set;

import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.annotation.Nullable;
import org.springframework.util.Assert;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Wrapper around a set of {@link IProperty properties} with simple routines to analyze and/or modify this set.
 * <p>
 * Caution: this wrapper does not monitor changes made to the backing set by other means. If the set is changed
 * outside, another instance of PropertySet should be built.
 *
 * @author Julien Canches
 */
public class PropertySet {

    private final Set<IProperty<?>> properties;

    /**
     * A map of property qualified names to "leaf" properties, i.e. all non-object properties either in the set or
     * embedded in an object property.
     */
    private final Map<String, IProperty<?>> flatProperties = new HashMap<>();

    /**
     * A map of property qualified names to object properties.
     */
    private final Map<String, ObjectProperty> objectProperties = new HashMap<>();

    public PropertySet(Set<IProperty<?>> properties) {
        this.properties = properties;
        new PropertiesVisitor(flatProperties::put, objectProperties::put).registerProperties(properties, null);
    }

    /**
     * Returns the qualified name of all leaf properties, including properties embedded in an object property.
     * The qualified name is the name of the property prefixed by its path, if it is embedded in an object.
     */
    public Set<String> getAllQualifiedNames() {
        return Collections.unmodifiableSet(flatProperties.keySet());
    }

    /**
     * Returns the property with specified qualified name.
     *
     * @param qualifiedName A property name, including its path if it is embedded in an object property.
     * @return The property with the qualified name, or null if no such property exists.
     */
    public @Nullable IProperty<?> get(String qualifiedName) {
        return flatProperties.get(qualifiedName);
    }

    /**
     * Listener called during {@link #mergeProperties(Set, MergeListener) merge} to monitor each
     * change made to the edited property set.
     */
    public interface MergeListener {

        void propertyAdded(String qualifiedName, Object value);

        void propertyUpdated(String qualifiedName, Object oldValue, Object newValue);

        void propertyRemoved(String qualifiedName, Object oldValue);
    }

    /**
     * Merge the properties of the given patch into this set. <b>This operation modifies the backing set</b>.
     *
     * @param patch    not <code>null</code> patch properties
     * @param listener listener called for each change to the original set
     */
    public void mergeProperties(Set<IProperty<?>> patch, MergeListener listener) {
        Assert.notNull(patch, "Patch properties must not be null");

        // Build a map of patch properties by qualified name
        Map<String, IProperty<?>> patchFlatProperties = new HashMap<>();
        new PropertiesVisitor(patchFlatProperties::put, (n, p) -> {
        }).registerProperties(patch, null);

        // Loop on patch properties
        for (Map.Entry<String, IProperty<?>> entry : patchFlatProperties.entrySet()) {
            IProperty<?> property = entry.getValue();
            String qualifiedName = entry.getKey();

            if (property.getValue() == null) {
                if (flatProperties.containsKey(qualifiedName)) {
                    // Unset property if exists
                    listener.propertyRemoved(qualifiedName, flatProperties.get(qualifiedName).getValue());
                    flatProperties.get(qualifiedName).updateValue(null);
                }
            } else {
                if (flatProperties.containsKey(qualifiedName)) {
                    listener.propertyUpdated(qualifiedName,
                                             flatProperties.get(qualifiedName).getValue(),
                                             property.getValue());
                    // Update property if already exists
                    IProperty.updatePropertyValue(flatProperties.get(qualifiedName), property.getValue());
                } else {
                    listener.propertyAdded(qualifiedName, property.getValue());
                    // Add property
                    Optional<String> namespace = getPropertyNamespace(qualifiedName);
                    if (namespace.isPresent()) {
                        getOrCreateObjectProperty(namespace.get()).addProperty(property);
                    } else {
                        properties.add(property);
                    }
                    flatProperties.put(qualifiedName, property);
                }
            }
        }
    }

    /**
     * Returns the object property with the specified qualified name, or create it if it does not exist.
     * If created, the object property is initially empty.
     */
    private ObjectProperty getOrCreateObjectProperty(String qualifiedName) {
        ObjectProperty ret = objectProperties.get(qualifiedName);
        // Don't use objectProperties.computeIfAbsent() because this method is recursive, and recursive calls may put
        // entries in the map.
        if (ret == null) {
            ret = IProperty.buildObject(getPropertyName(qualifiedName));
            Optional<String> namespace = getPropertyNamespace(qualifiedName);
            if (namespace.isPresent()) {
                getOrCreateObjectProperty(namespace.get()).addProperty(ret);
            } else {
                properties.add(ret);
            }
            objectProperties.put(qualifiedName, ret);
        }
        return ret;
    }

    private static Optional<String> getPropertyNamespace(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        return index == -1 ? Optional.empty() : Optional.of(qualifiedName.substring(0, index));
    }

    private static String getPropertyName(String qualifiedName) {
        int index = qualifiedName.lastIndexOf('.');
        return index == -1 ? qualifiedName : qualifiedName.substring(index + 1);
    }

    /**
     * Utility class to walk recursively through a collection of properties, including properties nested under an
     * object property.
     */
    private record PropertiesVisitor(BiConsumer<String, IProperty<?>> flatPropertyRegistration,
                                     BiConsumer<String, ObjectProperty> objectPropertyRegistration) {

        public void registerProperties(Collection<IProperty<?>> properties, String namespace) {
            for (IProperty<?> ppt : properties) {
                registerProperty(ppt, namespace);
            }
        }

        private void registerProperty(IProperty<?> property, String namespace) {
            String name = property.getName();
            String qualifiedName = namespace == null ? name : namespace + '.' + name;
            if (property.represents(PropertyType.OBJECT)) {
                objectPropertyRegistration.accept(qualifiedName, (ObjectProperty) property);
                registerProperties(((ObjectProperty) property).getValue(), qualifiedName);
            } else {
                flatPropertyRegistration.accept(qualifiedName, property);
            }
        }

    }

}
