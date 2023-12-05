package fr.cnes.regards.modules.ingest.dto.aip;

import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.geojson.AbstractFeatureCollection;

import java.util.Arrays;

/**
 * Collection of aips following geo json format
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPCollection extends AbstractFeatureCollection<AIPDto> {

    /**
     * Constructor setting the aips as features
     */
    public AIPCollection(AIPDto... aips) {
        super();
        getFeatures().addAll(Arrays.asList(aips));
    }

}
