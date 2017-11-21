package fr.cnes.regards.modules.storage.domain;

import java.util.Arrays;

import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPCollection extends AbstractFeatureCollection<AIP> {

    public AIPCollection(AIP... aips) {
        getFeatures().addAll(Arrays.asList(aips));
    }

}
