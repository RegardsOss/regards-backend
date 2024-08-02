/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY), without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.service.session;

import fr.cnes.regards.framework.modules.session.agent.domain.step.StepPropertyStateEnum;

/**
 * Enumeration for all product status in sessions.
 *
 * @author Sébastien Binda
 */
public enum SessionProductPropertyEnum {

    /**
     * Name of the property that collects the number of files acquired
     */
    PROPERTY_FILES_ACQUIRED("filesAcquired", StepPropertyStateEnum.SUCCESS, true, false),

    /**
     * Name of the property that collects the number of invalid files scanned
     */
    PROPERTY_FILES_INVALID("filesInvalid", StepPropertyStateEnum.INFO, false, false),

    /**
     * Name of the property that indicates if a chain is running
     */
    CHAIN_RUNNING("chainRunning", StepPropertyStateEnum.RUNNING, false, false),

    /**
     * Name of the property that collects the number of products incomplete
     */
    PROPERTY_INCOMPLETE("incomplete", StepPropertyStateEnum.INFO, false, false),

    /**
     * Name of the property indicating when a product was successfully created
     */
    PROPERTY_COMPLETED("complete", StepPropertyStateEnum.INFO, false, false),

    /**
     * Name of the property that collects the number of products invalid (too many files attached to a single product)
     */
    PROPERTY_INVALID("invalid", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Name of the property that collects the number of products generated
     */
    PROPERTY_GENERATED_PRODUCTS("generatedProducts", StepPropertyStateEnum.SUCCESS, false, true),

    /**
     * Name of the property that collects the number of products generated
     */
    PROPERTY_GENERATION_ERROR("generationError", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Name of the property that collects the number of ingestion fails after the sip submission
     */
    PROPERTY_INGESTION_FAILED("ingestionFailed", StepPropertyStateEnum.ERROR, false, false),

    /**
     * Name of the property indicating the sip has been successfully ingested
     */
    PROPERTY_INGESTED("ingested", StepPropertyStateEnum.INFO, false, false),

    /**
     * Name of the property indicating the sip ingestion has been canceled.
     */
    PROPERTY_INGESTION_CANCELED("canceled", StepPropertyStateEnum.INFO, false, true);

    private String name;

    private StepPropertyStateEnum state;

    private boolean inputRelated;

    private boolean outputRelated;

    SessionProductPropertyEnum(String name, StepPropertyStateEnum state, boolean inputRelated, boolean outputRelated) {
        this.name = name;
        this.state = state;
        this.inputRelated = inputRelated;
        this.outputRelated = outputRelated;
    }

    public String getName() {
        return name;
    }

    public StepPropertyStateEnum getState() {
        return state;
    }

    public boolean isInputRelated() {
        return inputRelated;
    }

    public boolean isOutputRelated() {
        return outputRelated;
    }
}
