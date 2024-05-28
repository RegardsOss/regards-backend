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
package fr.cnes.regards.framework.swagger.autoconfigure.override;

import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * To avoid add annotation to every converted attribute, here is a ModelConverter that automatically modify swagger
 * schema of the concerned attribute.
 * <p>
 * Use of Converter on an attribute implies that you must indicate to swagger the modified attribute type : example
 * <pre>
 * \@Convert(converter = PathAttributeConverter.class)   // -> return a String
 * \@Schema(implementation = String.class)               // -> precise to swagger the returned type of converter
 * private Path scannedDirectory;
 * </pre>
 * <p>
 * BE CAREFUL, this converter works only on attributes fields. In case of annotation @Convert on a class, you need to
 * precise the @Schema(implementation = ...) for swagger. This converter ModelConverter may by adapted in the future to
 * avoid that
 * <p>
 * This converter works on autoApplied converters. So attribute without annotation @Convert will be manage too, if
 * converter is auto apply.
 *
 * @author Thomas GUILLOU
 **/
public class ConverterToStringAwareConverter implements ModelConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConverterToStringAwareConverter.class);

    /**
     * List of type that is automagically serialized to string by a Converter
     */
    private List<Type> autoConvertedTypes;

    public ConverterToStringAwareConverter() {
        initAutoConvertedTypes();
    }

    private void initAutoConvertedTypes() {
        long start = System.currentTimeMillis();
        try {
            // find all converters auto applied and get converted types
            Stream<Type[]> genericTypesAutoConverted = new Reflections("fr.cnes.regards").getTypesAnnotatedWith(
                                                                                             Converter.class)
                                                                                         .stream()
                                                                                         .filter(clazz -> clazz.getAnnotation(
                                                                                                                   Converter.class)
                                                                                                               .autoApply())
                                                                                         .filter(clazz -> clazz.getAnnotatedInterfaces().length
                                                                                                          > 0)
                                                                                         .map(clazz -> ((ParameterizedType) clazz.getAnnotatedInterfaces()[0].getType()).getActualTypeArguments());

            // find those that serialize to String (AttributeConverter<?, String>)
            autoConvertedTypes = genericTypesAutoConverted.filter(genericTypes -> {
                Type typeConverted = genericTypes[1];
                return typeConverted.equals(String.class);
            }).map(genericTypes -> genericTypes[0]).toList();
        } catch (Exception e) {
            LOGGER.error("An error occurred while initializing auto converted types", e);
            autoConvertedTypes = List.of();
        }
        LOGGER.debug("Init auto converted types duration : {}", System.currentTimeMillis() - start);
    }

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            Schema<?> schema = chain.next().resolve(type, context, chain);
            // if type is known as automatic convert types, replace it by String
            // else check if this type is annotated with @Convert
            if (isTypeAutoConverted(type)) {
                schema.set$ref(null);
                schema.setType("string");
            } else {
                Annotation[] ctxAnnotations = type.getCtxAnnotations();
                if (ctxAnnotations != null && ctxAnnotations.length > 0) {
                    // search for @Convert annotation
                    Optional<Annotation> convertAnnotation = Stream.of(ctxAnnotations).filter(anno -> {
                        Class<? extends Annotation> annoType = anno.annotationType();
                        if (annoType != null) {
                            return annoType.equals(Convert.class);
                        }
                        return false;
                    }).findFirst();
                    convertAnnotation.ifPresent(annotation -> modifySchemaForConvertAnnotation(type,
                                                                                               schema,
                                                                                               (Convert) annotation));
                }
            }
            return schema;
        } else {
            return null;
        }
    }

    private boolean isTypeAutoConverted(AnnotatedType type) {
        if (type.getType() instanceof SimpleType) {
            return autoConvertedTypes.stream()
                                     .anyMatch(match -> type.getType().getTypeName().contains(match.getTypeName()));
        }
        return false;
    }

    private void modifySchemaForConvertAnnotation(AnnotatedType type, Schema<?> schema, Convert convert) {
        // if exists, get the converter attribute (which is a class)
        Class<?> converter = convert.converter();
        if (converter != null) {
            // find the generics types of the Converter<X,Y>
            java.lang.reflect.AnnotatedType[] annotatedInterfaces = converter.getAnnotatedInterfaces();
            if (annotatedInterfaces.length > 0) {
                ParameterizedType parameterizedType = (ParameterizedType) annotatedInterfaces[0].getType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                if (actualTypeArguments.length > 1) {
                    Type typeToConvert = actualTypeArguments[0];
                    Type typeConverted = actualTypeArguments[1];
                    // avoid the complex situation where the attribute type is different than the converter input
                    // like when you convert each User of a Set<User>, so only manage SimpleType (avoid CollectionType)
                    if (type.getType() instanceof SimpleType) {
                        Class<?> attributeClass = ((SimpleType) type.getType()).getRawClass();
                        if (Objects.equals(typeToConvert, attributeClass)) {
                            // if the return type is a string, change schema
                            if (typeConverted.equals(String.class)) {
                                schema.set$ref(null);
                                schema.setType("string");
                            }
                        }
                    }
                }
            }
        }
    }

}
