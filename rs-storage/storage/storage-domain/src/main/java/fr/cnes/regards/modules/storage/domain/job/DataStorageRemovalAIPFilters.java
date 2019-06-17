package fr.cnes.regards.modules.storage.domain.job;

import javax.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.Set;

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
