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
package fr.cnes.regards.framework.gson;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;

import org.reflections.Configuration;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonDiscriminator;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.Gsonable;

/**
 * Dynamic custom Gson annotation processor
 * @author Marc Sordi
 */
public final class GsonAnnotationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonAnnotationProcessor.class);

    private GsonAnnotationProcessor() {
    }

    /**
     * Utility method for building reflection context
     */
    private static Reflections getReflections(List<String> reflectionPackages) {

        // Initialize reflection tool
        Reflections reflections;
        if ((reflectionPackages == null) || reflectionPackages.isEmpty()) {
            String defaultPackage = "fr.cnes.regards";
            LOGGER.info("System will look for GSON adapters and factories in default package: {}", defaultPackage);
            reflections = new Reflections(defaultPackage);
        } else {
            StringJoiner customPackages = new StringJoiner(",");
            reflectionPackages.forEach(customPackages::add);
            LOGGER.info("System will look for GSON adapters and factories in default package: {}",
                        customPackages.toString());
            Configuration configuration = ConfigurationBuilder.build(reflectionPackages.toArray(new Object[0]));
            reflections = new Reflections(configuration);
        }
        return reflections;
    }

    /**
     * Customize GSON builder by reflection
     * @param builder {@link GsonBuilder}
     * @param reflectionPackages packages to scan
     */
    public static void process(GsonBuilder builder, List<String> reflectionPackages) {
        Reflections reflections = getReflections(reflectionPackages);
        processGsonable(builder, reflections);
        processGsonAdapterFactory(builder, reflections);
        processGsonAdapter(builder, reflections);
    }

    public static void process(GsonBuilder builder, String reflectionPackage) {
        process(builder, Collections.singletonList(reflectionPackage));
    }

    /**
    * Process all object annotated with {@link Gsonable} to dynamically create {@link PolymorphicTypeAdapterFactory}.
    */
    public static void processGsonable(GsonBuilder builder, String reflectionPackage) {
        processGsonable(builder, getReflections(Collections.singletonList(reflectionPackage)));
    }

    /**
    * Process all object annotated with {@link Gsonable} to dynamically create {@link PolymorphicTypeAdapterFactory}.
    */
    private static void processGsonable(GsonBuilder builder, Reflections reflections) {

        Set<Class<?>> gsonables = reflections.getTypesAnnotatedWith(Gsonable.class, true);

        if (gsonables != null) {
            for (Class<?> gsonable : gsonables) {

                // Retrieve annotation
                final Gsonable a = gsonable.getAnnotation(Gsonable.class);

                // Init factory
                final PolymorphicTypeAdapterFactory<?> typeAdapterFactory;
                if (a.value().isEmpty()) {
                    typeAdapterFactory = PolymorphicTypeAdapterFactory.of(gsonable);
                } else {

                    // Injection is always enabled with Gsonable so prevent field conflict
                    for (final Field field : gsonable.getDeclaredFields()) { // NOSONAR
                        if (field.getName().equals(a.value())) { // NOSONAR
                            // CHECKSTYLE:OFF
                            final String format = "Conflict between discriminator %s and %s field. Update or remove discriminator name.";
                            // CHECKSTYLE:ON
                            final String errorMessage = String.format(format, a.value(), gsonable);
                            LOGGER.error(errorMessage);
                            throw new IllegalArgumentException(errorMessage);
                        }
                    }

                    typeAdapterFactory = PolymorphicTypeAdapterFactory.of(gsonable, a.value(), true);
                }

                if (registerSubtypes(reflections, typeAdapterFactory, gsonable)) {
                    // Only register type with sub types
                    builder.registerTypeAdapterFactory(typeAdapterFactory);
                    LOGGER.info("GSON polymorphic type adapter factory registered : {}", typeAdapterFactory.getClass());
                }
            }
        }
    }

    /**
     * Register all sub types of the base type
     * @param reflections scanner
     * @param typeAdapterFactory current factory
     * @param baseType base hierarchy type
     * @return true if sub types are registered else false
     */
    private static boolean registerSubtypes(Reflections reflections,
            PolymorphicTypeAdapterFactory<?> typeAdapterFactory, Class<?> baseType) {

        final Set<?> subTypes = reflections.getSubTypesOf(baseType);

        if ((subTypes == null) || subTypes.isEmpty()) {
            // Skip registration
            LOGGER.warn("Skip registration of \"{}\". No sub type found!", baseType);
            return false;
        }

        for (Object subType : subTypes) {

            final Class<?> subClass = (Class<?>) subType;

            // Retrieve optional discriminator annotation
            final GsonDiscriminator discriminator = subClass.getAnnotation(GsonDiscriminator.class);

            if (discriminator == null) {
                // Register subtype without value - See registerSubtype for value computation
                typeAdapterFactory.registerSubtype(subClass);
            } else {
                typeAdapterFactory.registerSubtype(subClass, discriminator.value());
            }
        }

        return true;
    }

    /**
     * Process all object annotated with {@link GsonTypeAdapterFactory} to dynamically register
     * {@link TypeAdapterFactory}.
     */
    public static void processGsonAdapterFactory(GsonBuilder builder, String reflectionPackage) {
        processGsonAdapterFactory(builder, getReflections(Collections.singletonList(reflectionPackage)));
    }

    /**
     * Process all object annotated with {@link GsonTypeAdapterFactory} to dynamically register
     * {@link TypeAdapterFactory}.
     * @param builder {@link GsonBuilder}
     * @param pPrefix base package to scan
     */
    private static void processGsonAdapterFactory(GsonBuilder builder, Reflections reflections) {

        Set<Class<?>> factoryTypes = reflections.getTypesAnnotatedWith(GsonTypeAdapterFactory.class, true);

        if (factoryTypes != null) {
            for (Class<?> factoryType : factoryTypes) {

                if (!TypeAdapterFactory.class.isAssignableFrom(factoryType)) {
                    final String errorMessage = String.format("Factory %s must be an implementation of %s", factoryType,
                                                              TypeAdapterFactory.class);
                    LOGGER.error(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }

                // Create an instance
                @SuppressWarnings("unchecked")
                Class<? extends TypeAdapterFactory> factoryClass = (Class<? extends TypeAdapterFactory>) factoryType;
                try {
                    TypeAdapterFactory factory = factoryClass.newInstance();
                    builder.registerTypeAdapterFactory(factory);
                    LOGGER.info("GSON type adapter factory registered : {}", factory.getClass());
                } catch (InstantiationException | IllegalAccessException e) {
                    String format = "Factory %s cannot be instanciated. No arg public constructor must exist.";
                    final String errorMessage = String.format(format, factoryClass);
                    LOGGER.error(errorMessage, e);
                    throw new IllegalArgumentException(errorMessage);
                }
            }
        }
    }

    /**
     * Process all object annotated with {@link GsonTypeAdapterFactory} to dynamically register
     * {@link TypeAdapterFactory}.
     */
    public static void processGsonAdapter(GsonBuilder builder, String reflectionPackage) {
        processGsonAdapter(builder, getReflections(Collections.singletonList(reflectionPackage)));
    }

    /**
     * Process all object annotated with {@link GsonTypeAdapterFactory} to dynamically register
     * {@link TypeAdapterFactory}.
     */
    private static void processGsonAdapter(GsonBuilder builder, Reflections reflections) {

        Set<Class<?>> factoryTypes = reflections.getTypesAnnotatedWith(GsonTypeAdapter.class, true);

        if (factoryTypes != null) {
            for (Class<?> factoryType : factoryTypes) {

                if (!TypeAdapter.class.isAssignableFrom(factoryType)) {
                    final String errorMessage = String.format("Factory %s must be an implementation of %s", factoryType,
                                                              TypeAdapter.class);
                    LOGGER.error(errorMessage);
                    throw new IllegalArgumentException(errorMessage);
                }

                // Create an instance
                @SuppressWarnings("unchecked")
                Class<? extends TypeAdapter<?>> typeAdapterClass = (Class<? extends TypeAdapter<?>>) factoryType;
                GsonTypeAdapter annotation = typeAdapterClass.getAnnotation(GsonTypeAdapter.class);
                try {
                    TypeAdapter<?> adapter = typeAdapterClass.newInstance();
                    builder.registerTypeAdapter(annotation.adapted(), adapter);
                    LOGGER.info("GSON type adapter registered for type {}", annotation.adapted());
                } catch (InstantiationException | IllegalAccessException e) {
                    String format = "Factory %s cannot be instanciated. No arg public constructor must exist.";
                    final String errorMessage = String.format(format, typeAdapterClass);
                    LOGGER.error(errorMessage, e);
                    throw new IllegalArgumentException(errorMessage);
                }
            }
        }
    }
}
