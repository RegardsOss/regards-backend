package fr.cnes.regards.modules.storage.domain.job;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class DataStorageRemovalAIPFilters extends AIPQueryFilters {

    @NotEmpty
    private Set<Long> dataStorageIds = new HashSet<>();

    public Set<Long> getDataStorageIds() {
        return dataStorageIds;
    }

    public void setDataStorageIds(Set<Long> dataStorageIds) {
        this.dataStorageIds = dataStorageIds;
    }
}
