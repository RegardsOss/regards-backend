package fr.cnes.regards.modules.acquisition.domain.payload;

public enum UpdateAcquisitionProcessingChainType {
    /**
     * When editing only activity
     */
    ONLY_ACTIVITY,

    /**
     * When editing only the mode
     */
    ONLY_MODE,

    /**
     * When editing all field in the same time
     */
    ALL
}
