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
package fr.cnes.regards.modules.ltamanager.service.submission.creation;

import fr.cnes.regards.modules.ltamanager.dao.submission.ISubmissionRequestRepository;
import fr.cnes.regards.modules.ltamanager.domain.settings.DatatypeParameter;
import fr.cnes.regards.modules.ltamanager.domain.settings.LtaSettingsException;
import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestDto;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Create a valid datatype configuration from a {@link SubmissionRequestDto} for {@link SubmissionCreateService}
 *
 * @author Iliana Ghazali
 **/
@Service
public class CreateDatatypeService {

    private final ISubmissionRequestRepository requestRepository;

    public CreateDatatypeService(ISubmissionRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    /**
     * Create a valid datatype configuration for the {@link SubmissionRequestDto}
     *
     * @param requestDto      request dto submitted
     * @param datatypes       all datatypes settings configuration retrieved from the database
     * @param currentDateTime now in ISO-8601 calendar system
     * @return datatype configuration to use for the submission request dto
     * @throws LtaSettingsException if the configuration is invalid
     */
    DatatypeParameter createValidConfiguration(SubmissionRequestDto requestDto,
                                               Map<String, DatatypeParameter> datatypes,
                                               OffsetDateTime currentDateTime) throws LtaSettingsException {
        checkUniqueCorrelationId(requestDto.getCorrelationId());
        checkOwner(requestDto.getOwner());
        DatatypeParameter config = getDatatypeInConfig(requestDto.getDatatype(), datatypes);
        String storePath = checkAndGetStorePath(requestDto, config, currentDateTime).toString();
        return new DatatypeParameter(config.getModel(), storePath);

    }

    private void checkUniqueCorrelationId(String correlationId) throws LtaSettingsException {
        if (requestRepository.existsByCorrelationId(correlationId)) {
            throw new LtaSettingsException(String.format("Request with id \"%s\" is already registered in "
                                                         + "the database. Please provide a unique one.",
                                                         correlationId));
        }
    }

    /**
     * Check owner is present
     */
    private void checkOwner(String owner) throws LtaSettingsException {
        if (owner == null) {
            throw new LtaSettingsException("Owner of the request is required and must not be null.");
        }
    }

    /**
     * Retrieve the datatype configuration from the datatypes settings
     *
     * @param datatype  key that must correspond to an existing configuration provided by the {@link SubmissionRequestDto}
     * @param datatypes all datatypes settings configuration retrieved from the database
     * @return the corresponding datatype configuration
     * @throws LtaSettingsException if the datatype was not found
     */
    private DatatypeParameter getDatatypeInConfig(String datatype, Map<String, DatatypeParameter> datatypes)
        throws LtaSettingsException {
        DatatypeParameter datatypeConfig = datatypes.get(datatype);
        if (datatypeConfig == null) {
            throw new LtaSettingsException(String.format("""
                                                             The datatype "%s" was not found in the datatypes configuration.
                                                             Please make sure the datatype corresponds to an existing configuration.
                                                             """, datatype));
        }
        return datatypeConfig;
    }

    /**
     * Build a storePath from either the submission request dto or the datatype configuration if it is not present.
     * It the last case, it might be necessary to replace placeholders, for example :
     * ${YEAR}/${MONTH}/${DAY}/${PROPERTY(path1.to.property)}/${PROPERTY(path2.to.property)}
     *
     * @return the storePath
     * @throws LtaSettingsException if the storePath could not be built from the configuration
     */
    private Path checkAndGetStorePath(SubmissionRequestDto requestDto,
                                      DatatypeParameter config,
                                      OffsetDateTime currentDateTime) throws LtaSettingsException {
        String storePathDto = requestDto.getStorePath();
        try {
            if (storePathDto != null) {
                return Paths.get(storePathDto);
            } else {
                String builtStorePath = replaceProperties(requestDto.getProperties(), config.getStorePath());
                builtStorePath = replaceDates(builtStorePath, currentDateTime);
                checkBuiltStorePath(builtStorePath);
                return Paths.get(builtStorePath);
            }
        } catch (IllegalArgumentException e) {
            throw new LtaSettingsException(String.format(
                " - The storePath could not be built from the configuration \"%s\".%n"
                + "Either provide it in the SubmissionRequestDto or configure the storePath properly in the datatypes configuration.Cause:%n%s",
                config.getStorePath(),
                e.getMessage()), e);
        }
    }

    /**
     * Replace all optional placeholders starting with ${PROPERTY(jsonPath)} in the storePath configuration.
     * The placeholders point to properties to extract from the input {@link SubmissionRequestDto}.
     *
     * @param properties properties to be extracted from the input {@link SubmissionRequestDto}
     * @param storePath  storePath with optional placeholders
     * @return storePath with replaced placeholders
     * @throws IllegalArgumentException if the placeholders could not be replaced
     */
    private String replaceProperties(Map<String, Object> properties, String storePath) {
        // all properties contain in the storePath
        String[] propertiesToReplace = StringUtils.substringsBetween(storePath, "${PROPERTY(", ")}");

        // if no placeholders were detected return storePath as-is
        if (propertiesToReplace == null) {
            return storePath;
        }
        // Properties are required when placeholders have been detected in the storePath
        if (propertiesToReplace.length != 0 && properties == null) {
            throw new IllegalArgumentException(String.format("""
                                                                 Property placeholders to be replaced in the storePath "%s" have been detected but the properties of the SubmissionRequestDto are not present.
                                                                 Either provide properties in the submission request or remove properties placeholders in the storePath configuration.""",
                                                             storePath));
        }
        // Replace placeholders in storePath
        for (String propertyToReplace : propertiesToReplace) {
            try {
                String valueRetrieved = BeanUtils.getProperty(properties, propertyToReplace);
                storePath = replaceProperty(storePath, propertyToReplace, valueRetrieved);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new IllegalArgumentException(String.format("""
                                                                     The placeholders in the following storePath "%s" could not be replaced.
                                                                     Check if the json property "%s" exists in the properties of the SubmissionRequestDto.
                                                                     """, storePath, propertyToReplace), e);
            }
        }
        return storePath;
    }

    /**
     * Replace a specific property placeholder "${PROPERTY(jsonPath)}" in the storePath configuration
     *
     * @param storePath      storePath configuration with placeholders to be replaced
     * @param propertyPath   path pointing to the value to retrieve from the submission request dto properties
     * @param valueRetrieved value retrieved from the submission request dto properties.
     * @return storePath with the replaced placeholder
     * @throws IllegalArgumentException if the value to retrieve was not found
     */
    private String replaceProperty(String storePath, String propertyPath, String valueRetrieved) {
        if (valueRetrieved != null) {
            storePath = storePath.replaceFirst("\\$\\{PROPERTY\\([\\w+\\.]+\\)}", valueRetrieved);
        } else {
            throw new IllegalArgumentException(String.format("""
                                                                 The json key "%s" does not exist in the properties of the SubmissionRequestDto.
                                                                 Modify the storePath configuration to match existing json keys or verify the properties of the SubmissionRequestDto.
                                                                 """, propertyPath));
        }
        return storePath;
    }

    /**
     * Replace all optional date placeholders in the storePath.
     *
     * @param storePath storePath with optional placeholders
     * @return storePath with replaced placeholders
     */
    private String replaceDates(String storePath, OffsetDateTime currentDateTime) {
        String builtStorePath = storePath.replaceFirst("\\$\\{YEAR}", String.valueOf(currentDateTime.getYear()));
        builtStorePath = builtStorePath.replaceFirst("\\$\\{MONTH}", String.valueOf(currentDateTime.getMonthValue()));
        builtStorePath = builtStorePath.replaceFirst("\\$\\{DAY}", String.valueOf(currentDateTime.getDayOfMonth()));
        return builtStorePath;
    }

    /**
     * Check that built storePath contains only valid alphanumeric characters
     *
     * @param builtStorePath storePath to check
     */
    private void checkBuiltStorePath(String builtStorePath) {
        if (!Pattern.compile("^[\\w\\/\\-_:]*$").matcher(builtStorePath).matches()) {
            throw new IllegalArgumentException(String.format(
                "An error occurred while replacing placeholders in the storePath \"%s\"."
                + "It must contain only alphanumeric characters.",
                builtStorePath));
        }
    }

}
