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

import javax.persistence.Convert;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
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
 *
 * @author Thomas GUILLOU
 **/
public class ConverterToStringAwareConverter implements ModelConverter {

    @Override
    public Schema<?> resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            Schema<?> schema = chain.next().resolve(type, context, chain);
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
            return schema;
        } else {
            return null;
        }
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
