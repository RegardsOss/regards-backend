/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.toponyms.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.geolatte.geom.Geometry;
import org.geolatte.geom.Position;
import org.geolatte.geom.PositionSequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.coordinates.Positions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;

/**
 * Service to search {@link ToponymDTO}s from a postgis database
 *
 * @author Sébastien Binda
 *
 */
@Service
public class ToponymsService {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ToponymsService.class);

    /**
     * Maximum number of points to retrieve for each polygon of a geometry
     */
    private static final int POINT_SAMPLING_FINDALL = 50;

    @Autowired
    private ToponymsRepository repository;

    /**
     * Tolerance (unit: meter) to generate simplified geometry through  ST_Simplify Postgis function
     * @see https://postgis.net/docs/ST_Simplify.html
     */
    @Value("${regards.Toponyms.geo.sampling.tolerance:0.1}")
    private double tolerance;

    /**
     * Maximum number of points to retrieve for each polygon of a geometry
     * Default 0 for no sampling
     */
    @Value("${regards.Toponyms.geo.sampling.max.points:0}")
    private int sampling;

    /**
     * Retrieve {@link Page} of {@link ToponymDTO}s
     *
     * @param pageable
     * @return {@link ToponymDTO}s
     */
    public Page<ToponymDTO> findAll(Pageable pageable) {
        Page<Toponym> page = repository.findAll(pageable);
        return new PageImpl<ToponymDTO>(page.getContent().stream().map(f -> {
            return ToponymDTO.build(f.getBusinessId(), f.getLabel(), f.getLabelFr(),
                                    ToponymsService.parse(f.getGeometry(), POINT_SAMPLING_FINDALL), f.getCopyright(),
                                    f.getDescription());
        }).collect(Collectors.toList()), page.getPageable(), page.getTotalElements());
    }

    /**
     * Retrieve  one {@link ToponymDTO}s by his business unique identifier
     * @param businessId
     * @return {@link ToponymDTO}
     */
    public Optional<ToponymDTO> findOne(String businessId, boolean simplified) {
        if (!simplified) {
            Optional<Toponym> toponym = repository.findById(businessId);
            if (toponym.isPresent()) {
                Toponym t = toponym.get();
                return Optional.of(ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(),
                                                    ToponymsService.parse(t.getGeometry(), sampling), t.getCopyright(),
                                                    t.getDescription()));
            } else {
                return Optional.empty();
            }
        } else {
            // Optional<ISimplifiedToponym> toponym = repository.findOneSimplified(businessId);
            Optional<Toponym> toponym = repository.findOneSimplified(businessId, tolerance);
            if (toponym.isPresent()) {
                Toponym t = toponym.get();
                return Optional.of(ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(),
                                                    ToponymsService.parse(t.getGeometry(), sampling), t.getCopyright(),
                                                    t.getDescription()));
            } else {
                return Optional.empty();
            }
        }
    }

    /**
     * Search for toponyms matching the label and the locale given.
     * Returned {@link ToponymDTO}s are geometry free.
     * @param partialLabel
     * @param locale
     * @param limit maximum number of results to retrieve
     * @return {@link ToponymDTO}s without geometry
     */
    public List<ToponymDTO> search(String partialLabel, String locale, int limit) {
        Page<Toponym> page;
        Assert.notNull("locale is mandatory for toponyls search by label", locale);
        Assert.notNull("partialLabel is  mandatory for toponyms search by label", partialLabel);
        if (locale.equals(ToponymLocaleEnum.FR.getLocale())) {
            page = repository.findByLabelFrContainingIgnoreCase(partialLabel, PageRequest.of(0, limit));
        } else {
            page = repository.findByLabelContainingIgnoreCase(partialLabel, PageRequest.of(0, limit));
        }
        return page
                .getContent().stream().map(t -> ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(), null,
                                                                 t.getCopyright(), t.getDescription()))
                .collect(Collectors.toList());
    }

    /**
     * Parse a {@link Geometry} to build a {@link IGeometry}
     * @param geometry
     * @param samplingMax Maximum number of points to retrieve for each polygon of a geometry
     * @return {@link IGeometry}
     */
    private static IGeometry parse(Geometry<Position> geometry, int samplingMax) {
        IGeometry geo = null;
        switch (geometry.getGeometryType()) {
            case POLYGON:
                geo = new Polygon();
                geo.setCrs(geometry.getCoordinateReferenceSystem().getName());
                ((Polygon) geo)
                        .setCoordinates(parsePolygon((org.geolatte.geom.Polygon<Position>) geometry, samplingMax));
                break;
            case MULTIPOLYGON:
                geo = new MultiPolygon();
                List<fr.cnes.regards.framework.geojson.coordinates.PolygonPositions> postions = new ArrayList<fr.cnes.regards.framework.geojson.coordinates.PolygonPositions>();
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
                    position.getCoordinate(1), position.getCoordinate(3)));
        } else {
            LOGGER.error("Invalid dimension size " + position.getCoordinateDimension());
        }
    }

}
