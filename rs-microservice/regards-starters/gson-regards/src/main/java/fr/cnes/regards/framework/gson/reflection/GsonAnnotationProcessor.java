/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.gson.reflection;

import java.lang.reflect.Field;
import java.util.Set;

import org.reflections.Reflections;
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
import fr.cnes.regards.framework.gson.utils.GSONUtils;

/**
 * Dynamic {@link Gsonable} annotation processor
 *
 * @author Marc Sordi
 *
 */
public final class GsonAnnotationProcessor {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GsonAnnotationProcessor.class);

    private GsonAnnotationProcessor() {
    }

    /**
     * Process all object annotated with {@link Gsonable} to dynamically create
     * {@link PolymorphicTypeAdapterFactory}.<br/>
     * Process all {@link GsonTypeAdapterFactory} and {@link GsonTypeAdapter} to register this factories.
     *
     * @param pBuilder {@link GsonBuilder}
     * @param pPrefix base package to scan
     */
    public static void process(GsonBuilder pBuilder, String pPrefix) {
        processGsonable(pBuilder, pPrefix);
        processGsonAdapterFactory(pBuilder, pPrefix);
        processGsonAdapter(pBuilder, pPrefix);
    }

    /**
     * Process all object annotated with {@link Gsonable} to dynamically create {@link PolymorphicTypeAdapterFactory}.
     *
     * @param pBuilder
     *            {@link GsonBuilder}
     * @param pPrefix
     *            base package to scan
     */
    public static void processGsonable(GsonBuilder pBuilder, String pPrefix) {
        GSONUtils.assertNotNull(pBuilder, "GSON builder is required");
        GSONUtils.assertNotNullOrEmpty(pPrefix, "Prefix package to scan is required");

        // Scan for Gsonable
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> gsonables = reflections.getTypesAnnotatedWith(Gsonable.class, true);

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
                    pBuilder.registerTypeAdapterFactory(typeAdapterFactory);
                }
            }
        }
    }

    /**
     * Register all sub types of the base type
     *
     * @param pReflections
     *            scanner
     * @param pTypeAdapterFactory
     *            current factory
     * @param pBaseType
     *            base hierarchy type
     * @return true if sub types are registered else false
     */
    private static boolean registerSubtypes(Reflections pReflections,
            PolymorphicTypeAdapterFactory<?> pTypeAdapterFactory, Class<?> pBaseType) {

        final Set<?> subTypes = pReflections.getSubTypesOf(pBaseType);

        if ((subTypes == null) || subTypes.isEmpty()) {
            // Skip registration
            LOGGER.warn("Skip registration of \"{}\". No sub type found!", pBaseType);
            return false;
        }

        for (Object subType : subTypes) {

            final Class<?> subClass = (Class<?>) subType;

            // Retrieve optional discriminator annotation
            final GsonDiscriminator discriminator = subClass.getAnnotation(GsonDiscriminator.class);

            if (discriminator == null) {
                // Register subtype without value - See registerSubtype for value computation
                pTypeAdapterFactory.registerSubtype(subClass);
            } else {
                pTypeAdapterFactory.registerSubtype(subClass, discriminator.value());
            }
        }

        return true;
    }

    /**
     * Process all object annotated with {@link GsonTypeAdapterFactory} to dynamically register
     * {@link TypeAdapterFactory}.
     *
     * @param pBuilder
     *            {@link GsonBuilder}
     * @param pPrefix
     *            base package to scan
     */
    public static void processGsonAdapterFactory(GsonBuilder pBuilder, String pPrefix) {

        // Scan for Gsonable
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> factoryTypes = reflections.getTypesAnnotatedWith(GsonTypeAdapterFactory.class, true);

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
                    pBuilder.registerTypeAdapterFactory(factory);
                } catch (InstantiationException | IllegalAccessException e) {
                    String format = "Factory % cannot be instanciated. No arg public constructor must exist.";
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
     *
     * @param pBuilder
     *            {@link GsonBuilder}
     * @param pPrefix
     *            base package to scan
     */
    public static void processGsonAdapter(GsonBuilder pBuilder, String pPrefix) {

        // Scan for Gsonable
        final Reflections reflections = new Reflections(pPrefix);
        final Set<Class<?>> factoryTypes = reflections.getTypesAnnotatedWith(GsonTypeAdapter.class, true);

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
                    pBuilder.registerTypeAdapter(annotation.adapted(), adapter);
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
