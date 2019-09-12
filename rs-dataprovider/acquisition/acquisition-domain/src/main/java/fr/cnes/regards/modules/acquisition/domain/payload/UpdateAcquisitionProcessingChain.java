package fr.cnes.regards.modules.acquisition.domain.payload;

import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChainMode;


public class UpdateAcquisitionProcessingChain {
    private Boolean active = false;

    private AcquisitionProcessingChainMode mode = AcquisitionProcessingChainMode.MANUAL;

    private UpdateAcquisitionProcessingChainType updateType;

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public AcquisitionProcessingChainMode getMode() {
        return mode;
    }

    public void setMode(AcquisitionProcessingChainMode mode) {
        this.mode = mode;
    }

    public UpdateAcquisitionProcessingChainType getUpdateType() {
        return updateType;
    }

    public void setUpdateType(UpdateAcquisitionProcessingChainType updateType) {
        this.updateType = updateType;
    }

    public static UpdateAcquisitionProcessingChain build(Boolean active, AcquisitionProcessingChainMode mode, UpdateAcquisitionProcessingChainType updateType) {
        UpdateAcquisitionProcessingChain updateQuery = new UpdateAcquisitionProcessingChain();
        updateQuery.setActive(active);
        updateQuery.setMode(mode);
        updateQuery.setUpdateType(updateType);
        return updateQuery;
    }
}
