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
package fr.cnes.regards.framework.modules.jobs.domain.converters;

import java.io.IOException;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.framework.modules.jobs.domain.Output;

/**
 * 
 * @author Léo Mieulet
 * @author Christophe Mertz
 *
 */
@Converter(autoApply = true)
public class JobOuputConverter implements AttributeConverter<List<Output>, String> {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(JobOuputConverter.class);

    /**
     * Jackson mapper
     */
    private final ObjectMapper mapper = new ObjectMapper(); // jackson's objectmapper

    @Override
    public String convertToDatabaseColumn(final List<Output> pOutput) {
        String json = "";
        if (pOutput != null) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
            try {
                json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(pOutput);
            } catch (final IOException e) {
                LOG.error("Failed to convert JobOutput POJO to string", e);
            }
        }
        return json;
    }

    @Override
    public List<Output> convertToEntityAttribute(final String pDbData) {
        List<Output> result = null;
        if (pDbData != null && !pDbData.isEmpty()) {
            mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

            try {
                result = mapper.readValue(pDbData, new TypeReference<List<Output>>() {
                });
            } catch (final IOException e) {
                LOG.error("Failed to convert JobOutput persisted string to POJO", e);
            }
        }
        return result;
    }

}