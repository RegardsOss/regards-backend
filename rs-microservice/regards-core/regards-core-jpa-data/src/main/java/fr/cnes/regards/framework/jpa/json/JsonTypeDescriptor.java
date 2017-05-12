/*
 * LICENSE_PLACEHOLDER
 */
/*
 * Copyright 2015 Vlad Mihalcea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.Type;
import java.util.Properties;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.AbstractTypeDescriptor;
import org.hibernate.usertype.DynamicParameterizedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vlad Mihalcea
 */
@SuppressWarnings("serial")
public class JsonTypeDescriptor extends AbstractTypeDescriptor<Object> implements DynamicParameterizedType {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonTypeDescriptor.class);

    /**
     * Allows to specify argument type for generics to help for deserialization.<br/>
     * Must be set when declaring a jsonb field : <br/>
     * Its value corresponds to the target name class that will be retrieved using {@link Class#forName(String)}<br/>
     * <code>
     * @Type(type = "jsonb", parameters = { @Parameter(name = JsonTypeDescriptor.ARG_TYPE, value = "{classname}") })
     * </code>
     */
    public static final String ARG_TYPE = "fr.cnes.regards.ParameterType.argType";

    /**
     * JAVA object type : may be simple class or parameterized type
     */
    private Type type;

    public JsonTypeDescriptor() {
        super(Object.class, new JsonBinaryMutableMutabilityPlan());
    }

    @Override
    public void setParameterValues(Properties parameters) {
        // Manage parameterized type
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Managing json type for entity {} and property {}", parameters.get(ENTITY),
                         parameters.get(PROPERTY));
        }

        // Get raw type
        Type rawType = ((ParameterType) parameters.get(PARAMETER_TYPE)).getReturnedClass();

        // Check if argument type is specified for generics
        Object argTypeName = parameters.get(ARG_TYPE);

        // Compute type to deserialize
        if (argTypeName != null) {
            Type argType;
            try {
                argType = Class.forName((String) argTypeName);
            } catch (ClassNotFoundException e) {
                String message = String.format(
                                               "Argument type name %s does not correspond to a valid class on the classpath",
                                               argTypeName);
                LOGGER.error(message, e);
                throw new IllegalArgumentException(e);
            }
            // Define a parameterized type
            type = new ParameterizedTypeImpl(null, rawType, argType);
        } else {
            type = rawType;
        }

        // Propagate to plan
        ((JsonBinaryMutableMutabilityPlan) getMutabilityPlan()).setType(type);

    }

    @Override
    public boolean areEqual(Object one, Object another) {
        if (one == another) {
            return true;
        }
        if ((one == null) || (another == null)) {
            return false;
        }
        return GsonUtil.toJsonNode(GsonUtil.toString(one)).equals(GsonUtil.toJsonNode(GsonUtil.toString(another)));
    }

    public Object clone(Object value) {
        return null;
    }

    @Override
    public String toString(Object value) {
        return GsonUtil.toString(value);
    }

    @Override
    public Object fromString(String string) {
        return GsonUtil.fromString(string, type);
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public <X> X unwrap(Object value, Class<X> type, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        if (String.class.isAssignableFrom(type)) {
            return (X) toString(value);
        }
        if (Object.class.isAssignableFrom(type)) {
            return (X) GsonUtil.toJsonNode(value);
        }
        throw unknownUnwrap(type);
    }

    @Override
    public <X> Object wrap(X value, WrapperOptions options) {
        if (value == null) {
            return null;
        }
        return fromString(value.toString());
    }

}
