package fr.cnes.regards.framework.modules.session.commons.domain;

/**
 * {@link SessionStep} type
 *
 * @author Iliana Ghazali
 **/
public enum StepTypeEnum {

    /**
     * For steps originating from dataprovider and feature provider
     */
    ACQUISITION,
    /**
     * For steps originating from ingest and feature manager
     */
    REFERENCING,
    /**
     * For steps originating from storage
     */
    STORAGE,
    /**
     * For steps originating from dam
     */
    DISSEMINATION;
}