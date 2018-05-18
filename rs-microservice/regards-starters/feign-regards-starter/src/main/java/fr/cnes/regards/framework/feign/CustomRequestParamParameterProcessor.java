/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.feign;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cloud.netflix.feign.AnnotatedParameterProcessor;
import org.springframework.cloud.netflix.feign.annotation.RequestParamParameterProcessor;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.common.base.Strings;
import feign.MethodMetadata;

/**
 * Improve the default {@link RequestParamParameterProcessor} in order to teach it how to handle Map<String, String> parameters in REST controllers,
 * @author Sébastien Binda
 * @author Xavier-Alexandre Brochard
 */
public class CustomRequestParamParameterProcessor implements AnnotatedParameterProcessor {

    private static final Class<RequestParam> ANNOTATION = RequestParam.class;

    @Override
    public Class<? extends Annotation> getAnnotationType() {
        return ANNOTATION;
    }

    @Override
    public boolean processArgument(AnnotatedParameterContext pContext, Annotation pAnnotation, Method pMethod) {
        String name = ANNOTATION.cast(pAnnotation).value();
        if (!Strings.isNullOrEmpty(name)) {
            pContext.setParameterName(name);

            MethodMetadata data = pContext.getMethodMetadata();
            Collection<String> query = pContext.setTemplateParameter(name, data.template().queries().get(name));
            data.template().query(name, query);
        } else {
            MethodMetadata data = pContext.getMethodMetadata();
            data.queryMapIndex(pContext.getParameterIndex());
        }
        return true;
    }

}
