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
package fr.cnes.regards.modules.toponyms;

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

import fr.cnes.regards.framework.geojson.coordinates.PolygonPositions;
import fr.cnes.regards.framework.geojson.geometry.IGeometry;
import fr.cnes.regards.framework.geojson.geometry.MultiPolygon;
import fr.cnes.regards.framework.geojson.geometry.Polygon;

/**
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

    private static final int POINT_SAMPLING_FINDALL = 50;

    @Autowired
    private ToponymsRepository repository;

    @Value("${regards.Toponyms.geo.sampling.max:0}")
    private int samplingMax;

    /**
     * @param pageable
     * @return
     */
    public Page<ToponymDTO> findAll(Pageable pageable) {
        Page<Toponym> page = repository.findAll(pageable);
        return new PageImpl<ToponymDTO>(page.getContent().stream().map(f -> {
            return ToponymDTO.build(f.getBusinessId(), f.getLabel(), f.getLabelFr(),
                                    ToponymsService.parse(f.getGeometry(), POINT_SAMPLING_FINDALL), f.getCopyright(),
                                    f.getDescription());
        }).collect(Collectors.toList()), page.getPageable(), page.getTotalElements());
    }

    public Optional<ToponymDTO> findOne(String businessId) {
        Optional<Toponym> Toponym = repository.findById(businessId);
        if (Toponym.isPresent()) {
            Toponym t = Toponym.get();
            return Optional.of(ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(),
                                                ToponymsService.parse(t.getGeometry(), samplingMax), t.getCopyright(),
                                                t.getDescription()));
        } else {
            return Optional.empty();
        }
    }

    public List<ToponymDTO> search(String partialLabel, String locale, int limit) {
        Page<Toponym> page;
        if (locale.equals("fr")) {
            page = repository.findByLabelFrContainingIgnoreCase(partialLabel, PageRequest.of(0, limit));
        } else {
            page = repository.findByLabelContainingIgnoreCase(partialLabel, PageRequest.of(0, limit));
        }
        return page
                .getContent().stream().map(t -> ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(), null,
                                                                 t.getCopyright(), t.getDescription()))
                .collect(Collectors.toList());
    }

    public static IGeometry parse(Geometry<Position> geometry, int samplingMax) {
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

    private static void addPositionsToRing(PositionSequence<Position> positions,
            fr.cnes.regards.framework.geojson.coordinates.Positions ring, int maxSampling) {
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

    private static void addPositionToRing(Position position,
            fr.cnes.regards.framework.geojson.coordinates.Positions ringPosition) {
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
