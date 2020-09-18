package fr.cnes.regards.modules.processing.order;

import fr.cnes.regards.framework.urn.DataType;
import io.vavr.collection.List;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.With;

@With @Value @AllArgsConstructor
public class OrderProcessInfo {

    Scope scope;
    Cardinality cardinality;
    List<DataType> requiredDatatypes;

}
