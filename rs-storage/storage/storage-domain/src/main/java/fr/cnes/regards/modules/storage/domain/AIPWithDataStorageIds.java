package fr.cnes.regards.modules.storage.domain;

import java.util.HashSet;
import java.util.Set;

/**
 * DTO used on rest layer.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPWithDataStorageIds {

    private AIP aip;

    private Set<Long> dataStorageIds = new HashSet<>();

    public AIPWithDataStorageIds(AIP aip, Set<Long> dataStorageIds) {
        this.aip = aip;
        this.dataStorageIds = dataStorageIds;
    }

    public Set<Long> getDataStorageIds() {
        return dataStorageIds;
    }

    public void setDataStorageIds(Set<Long> dataStorageIds) {
        this.dataStorageIds = dataStorageIds;
    }

    public AIP getAip() {
        return aip;
    }

    public void setAip(AIP aip) {
        this.aip = aip;
    }
}
