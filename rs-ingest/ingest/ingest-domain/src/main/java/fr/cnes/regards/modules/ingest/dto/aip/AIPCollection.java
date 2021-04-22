package fr.cnes.regards.modules.ingest.dto.aip;

import java.util.Arrays;

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;

/**
 * Collection of aips following geo json format
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPCollection extends AbstractFeatureCollection<AIP> {

    /**
     * Constructor setting the aips as features
     * @param aips
     */
    public AIPCollection(AIP... aips) {
        super();
        getFeatures().addAll(Arrays.asList(aips));
    }

}
