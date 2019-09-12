package fr.cnes.regards.modules.acquisition.domain.payload;

import java.util.List;

public class UpdateAcquisitionProcessingChains extends UpdateAcquisitionProcessingChain {

    private List<Long> chainIds;

    public List<Long> getChainIds() {
        return chainIds;
    }

    public void setChainIds(List<Long> chainIds) {
        this.chainIds = chainIds;
    }

    public static UpdateAcquisitionProcessingChains build(List<Long> chainIds,
            UpdateAcquisitionProcessingChain updateAcquisitionProcessingChain) {
        UpdateAcquisitionProcessingChains updateQuery = new UpdateAcquisitionProcessingChains();
        updateQuery.setChainIds(chainIds);
        updateQuery.setActive(updateAcquisitionProcessingChain.getActive());
        updateQuery.setMode(updateAcquisitionProcessingChain.getMode());
        updateQuery.setUpdateType(updateAcquisitionProcessingChain.getUpdateType());
        return updateQuery;
    }
}
