package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.modules.dam.domain.entities.AbstractEntity;
import lombok.Value;
import org.elasticsearch.search.aggregations.Aggregation;

import java.util.List;

/**
 * This class wrap collection information found with URN and DataObjects' aggregation data contained in this collection
 */
@Value
public class CollectionWithStats {
    private AbstractEntity collection;
    private List<Aggregation> aggregationList;

}
