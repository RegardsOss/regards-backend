package fr.cnes.regards.modules.toponyms.service.utils;

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import java.util.ArrayList;
import java.util.List;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.Position;
import org.geolatte.geom.PositionSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility to convert a {@link Geometry} to a {@link IGeometry}
 *
 * @author Sébastien Binda
 */
public class ToponymsIGeometryHelper {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ToponymsIGeometryHelper.class);


    /**
     * Parse a {@link Geometry} to build a {@link IGeometry}
     * @param geometry
     * @param samplingMax Maximum number of points to retrieve for each polygon of a geometry
     * @return {@link IGeometry}
     */
    public static IGeometry parseLatteGeometry(Geometry<Position> geometry, int samplingMax) {
        IGeometry geo = null;
        if (geometry != null) {
            switch (geometry.getGeometryType()) {
                case POLYGON:
                    geo = new Polygon();
                    geo.setCrs(geometry.getCoordinateReferenceSystem().getName());
                    ((Polygon) geo)
                            .setCoordinates(parsePolygon((org.geolatte.geom.Polygon<Position>) geometry, samplingMax));
                    break;
                case MULTIPOLYGON:
                    geo = new MultiPolygon();
                    List<PolygonPositions> postions = new ArrayList<PolygonPositions>();
                    org.geolatte.geom.MultiPolygon<Position> gPol = (org.geolatte.geom.MultiPolygon<Position>) geometry;
                    // Loop over each polygon
                    for (int i = 0; i < gPol.getNumGeometries(); i++) {
                        // Parse polygon positions
                        postions.add(parsePolygon(gPol.getGeometryN(i), samplingMax));
                    }
                    ((MultiPolygon) geo).setCoordinates(postions);
                    geo.setCrs(geometry.getCoordinateReferenceSystem().getName());
                    break;
                case CURVE:
                case GEOMETRYCOLLECTION:
                case LINEARRING:
                case LINESTRING:
                case MULTILINESTRING:
                case MULTIPOINT:
                case POINT:
                case SURFACE:
                default:
                    LOGGER.error("Geometry type {} not handled yet !", geometry.getGeometryType());
                    break;
            }
        }
        return geo;
    }


    /**
     * Parse a {@link org.geolatte.geom.Polygon} to build a {@link PolygonPositions}
     * @param polygon
     * @param samplingMax Maximum number of points to retrieve for each polygon of a geometry
     * @return {@link PolygonPositions}
     */
    private static PolygonPositions parsePolygon(org.geolatte.geom.Polygon<Position> polygon, int samplingMax) {
        // Create result IGeometry#Polygon
        PolygonPositions polygonPostions = new PolygonPositions();
        // Create result positions for Polygin external ring positions
        fr.cnes.regards.framework.geojson.coordinates.Positions exteriorRingPostions = new fr.cnes.regards.framework.geojson.coordinates.Positions();
        // Add external Ring positions
        addPositionsToRing(polygon.getExteriorRing().getPositions(), exteriorRingPostions, samplingMax);
        polygonPostions.add(0, exteriorRingPostions);
        // Loop over internal rings to add assiociated positions
        for (int j = 0; j < polygon.getNumInteriorRing(); j++) {
            fr.cnes.regards.framework.geojson.coordinates.Positions ineriorRing = new fr.cnes.regards.framework.geojson.coordinates.Positions();
            addPositionsToRing(polygon.getInteriorRingN(j).getPositions(), ineriorRing, samplingMax);
            polygonPostions.add(j + 1, ineriorRing);
        }
        return polygonPostions;
    }

    /**
     * Parse a {@link PositionSequence} to add each included {@link Position} in the given {@link Positions}
     * @param positions {@link PositionSequence} to  parse
     * @param ring {@link Positions}
     * @param maxSampling Maximum number of points to retrieve for each polygon of a geometry
     */
    private static void addPositionsToRing(PositionSequence<Position> positions, Positions ring, int maxSampling) {
        int step = (maxSampling > 2) && (maxSampling < positions.size()) ? ((positions.size() / maxSampling)) : 1;
        int index;
        boolean last = false;
        for (index = 0; index < positions.size(); index = index + step) {
            addPositionToRing(positions.getPositionN(index), ring);
            last = index == (positions.size() - 1);
        }
        // Ensure add last point
        if (!last) {
            addPositionToRing(positions.getPositionN(positions.size() - 1), ring);
        }
        LOGGER.debug("Ring sampled {}/{} (step={})", ring.size(), positions.size(), step);
    }

    /**
     * Parse a {@link Position} to build a {@link fr.cnes.regards.framework.geojson.coordinates.Position} and add it in the given {@link Positions}
     * @param position {@link Position} to parse
     * @param ringPosition {@link Positions} to add the built {@link fr.cnes.regards.framework.geojson.coordinates.Position}
     */
    private static void addPositionToRing(Position position, Positions ringPosition) {
        // Add  positions with right dimension
        if (position.getCoordinateDimension() == 2) {
            ringPosition.add(new fr.cnes.regards.framework.geojson.coordinates.Position(position.getCoordinate(0),
                    position.getCoordinate(1)));
        } else if (position.getCoordinateDimension() == 3) {
            ringPosition.add(new fr.cnes.regards.framework.geojson.coordinates.Position(position.getCoordinate(0),
                    position.getCoordinate(1), position.getCoordinate(2)));
        } else {
            LOGGER.error("Invalid dimension size " + position.getCoordinateDimension());
        }
    }
}
