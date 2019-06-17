package fr.cnes.regards.modules.storage.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;

import fr.cnes.regards.modules.storage.domain.database.PrioritizedDataStorage;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPPageWithDataStorages extends PagedResources<AIPWithDataStorageIds> {

    private Set<PrioritizedDataStorage> dataStorages = new HashSet<>();

    public AIPPageWithDataStorages(Set<PrioritizedDataStorage> dataStorages, Collection<AIPWithDataStorageIds> content, PageMetadata metadata, Link... links) {
        super(content, metadata, links);
        this.dataStorages = dataStorages;
    }

    public AIPPageWithDataStorages(Set<PrioritizedDataStorage> dataStorages, Collection<AIPWithDataStorageIds> content, PageMetadata metadata,
            Iterable<Link> links) {
        super(content, metadata, links);
        this.dataStorages = dataStorages;
    }

    public Set<PrioritizedDataStorage> getDataStorages() {
        return dataStorages;
    }

    public void setDataStorages(Set<PrioritizedDataStorage> dataStorages) {
        this.dataStorages = dataStorages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        AIPPageWithDataStorages that = (AIPPageWithDataStorages) o;

        return dataStorages.equals(that.dataStorages);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + dataStorages.hashCode();
        return result;
    }
}
