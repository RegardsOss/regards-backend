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
package fr.cnes.regards.modules.search.rest;

import javax.xml.bind.annotation.XmlSchema;

import org.springframework.http.MediaType;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;

import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

import fr.cnes.regards.modules.search.schema.OpenSearchDescription;

/**
 * As JaxB annotation module for Jackson converter does not handle XmlSchema annotation We force use of JaxBconverter
 * to read/write OpensearchDescriptor classes.
 *
 * @see {@link JaxbAnnotationIntrospector} {@link XmlSchema} not handled
 *
 * @author Sébastien Binda
 *
 */
public class OpensSearchJaxbConverter extends Jaxb2RootElementHttpMessageConverter {

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return supports(clazz) && super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return supports(clazz) && super.canWrite(clazz, mediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return clazz.equals(OpenSearchDescription.class);
    }

}
