/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.adapters.gson;

import fr.cnes.regards.framework.gson.adapters.PolymorphicTypeAdapterFactory;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterFactory;
import fr.cnes.regards.modules.indexer.domain.facet.DateFacet;
import fr.cnes.regards.modules.indexer.domain.facet.FacetType;
import fr.cnes.regards.modules.indexer.domain.facet.IFacet;
import fr.cnes.regards.modules.indexer.domain.facet.NumericFacet;
import fr.cnes.regards.modules.indexer.domain.facet.StringFacet;

/**
 * Facet adapter factory
 *
 * @author Xavier-Alenxandre Brochard
 */
@SuppressWarnings("rawtypes")
@GsonTypeAdapterFactory
public class FacetAdapterFactory extends PolymorphicTypeAdapterFactory<IFacet> {

    /**
     * Constructor
     */
    public FacetAdapterFactory() {
        super(IFacet.class, "type", true);
        registerSubtype(StringFacet.class, FacetType.STRING);
        registerSubtype(NumericFacet.class, FacetType.NUMERIC);
        registerSubtype(DateFacet.class, FacetType.DATE);
    }
}
