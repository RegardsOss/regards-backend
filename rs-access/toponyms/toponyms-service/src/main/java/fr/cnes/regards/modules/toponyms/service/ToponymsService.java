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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.EntityAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.modules.toponyms.dao.ToponymsRepository;
import fr.cnes.regards.modules.toponyms.domain.Toponym;
import fr.cnes.regards.modules.toponyms.domain.ToponymDTO;
import fr.cnes.regards.modules.toponyms.domain.ToponymLocaleEnum;
import fr.cnes.regards.modules.toponyms.domain.ToponymMetadata;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotHandledException;
import fr.cnes.regards.modules.toponyms.service.exceptions.GeometryNotProcessedException;
import fr.cnes.regards.modules.toponyms.service.exceptions.MaxLimitPerDayException;
import fr.cnes.regards.modules.toponyms.service.utils.ToponymsIGeometryHelper;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.geolatte.geom.Feature;
import org.geolatte.geom.Geometry;
import org.geolatte.geom.GeometryType;
import org.geolatte.geom.Position;
import org.geolatte.geom.json.GeolatteGeomModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Service to search {@link ToponymDTO}s from a postgis database
 *
 * @author Sébastien Binda
 */
@Service
@RegardsTransactional
public class ToponymsService {

    /**
     * Toponyms repository
     */
    @Autowired
    private ToponymsRepository repository;


    // --- DEFAULT PARAMETERS ---

    /**
     * Maximum number of toponyms that can be saved per day and per user
     */
    @Value("${regards.toponyms.limit.save:30}")
    private int limitSave;

    /**
     * Tolerance (unit: meter) to generate simplified geometry through  ST_Simplify Postgis function
     * @see "https://postgis.net/docs/ST_Simplify.html"
     */
    @Value("${regards.toponyms.geo.sampling.tolerance:0.1}")
    private double tolerance;

    /**
     * Maximum number of points to retrieve for each polygon of a geometry
     * Default 0 for no sampling
     */
    @Value("${regards.toponyms.geo.sampling.max.points:0}")
    private int sampling;

    /**
     * Parameter used to set the expiration date of a not visible toponym
     */
    @Value("${regards.toponyms.expiration:30}")
    private int defaultExpiration;

    /**
     * Maximum number of points to retrieve for each polygon of a geometry
     */
    private static final int POINT_SAMPLING_FINDALL = 50;


    /**
     * Retrieve {@link Page} of {@link ToponymDTO}s
     * @param visible visibility of the toponym
     * @param pageable
     * @return {@link ToponymDTO}s
     */
    public Page<ToponymDTO> findAllByVisibility(String locale, boolean visible, Pageable pageable) {
        Pageable page;
        if (locale.equals(ToponymLocaleEnum.FR.getLocale())) {
            page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Direction.ASC, "labelFr"));
        } else {
            page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Direction.ASC, "label"));
        }
        Page<Toponym> toponymsPage = repository.findByVisible(visible, page);
        return new PageImpl<ToponymDTO>(toponymsPage.getContent().stream().map(t ->
                getToponymDTO(t, POINT_SAMPLING_FINDALL)).collect(Collectors.toList()), toponymsPage.getPageable(), toponymsPage.getTotalElements());
    }

    /**
     * Retrieve one {@link ToponymDTO}s by his business unique identifier. If the toponym is not visible, the lastAccessDate
     * will automatically be updated to now.
     *
     * @param businessId the identifier of the toponym searched
     * @param simplified if the geometry has to be returned simplified, i.e, with a tolerance)
     * @return {@link ToponymDTO}
     */
     public Optional<ToponymDTO> findOne(String businessId, boolean simplified) {
        Optional<Toponym> toponym;
        // check if geometry should be returned simplified
        if (!simplified) {
            toponym = repository.findById(businessId);
        } else {
            // Optional<ISimplifiedToponym> toponym = repository.findOneSimplified(businessId);
            toponym = repository.findOneSimplified(businessId, tolerance);
        }
        // check if the toponym is present and its visibility
        if (toponym.isPresent()) {
            Toponym t = toponym.get();
            if (!t.isVisible()) {
                t.getToponymMetadata().setExpirationDate(OffsetDateTime.now().plusDays(this.defaultExpiration));
                t = this.repository.save(t);
            }
            return Optional.of(getToponymDTO(t, sampling));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Search for toponyms matching the label and the locale given.
     * Returned {@link ToponymDTO}s are geometry free.
     *
     * @param partialLabel
     * @param locale
     * @param visible
     * @param limit maximum number of results to retrieve
     * @return {@link ToponymDTO}s without geometry
     */
    public List<ToponymDTO> search(String partialLabel, String locale, boolean visible, int limit) {
        Page<Toponym> page;
        Assert.notNull("locale is mandatory for toponyls search by label", locale);
        Assert.notNull("partialLabel is  mandatory for toponyms search by label", partialLabel);
        if (locale.equals(ToponymLocaleEnum.FR.getLocale())) {
            page = repository
                    .findByLabelFrContainingIgnoreCaseAndVisible(partialLabel, visible,
                            PageRequest.of(0, limit, Sort.by(Direction.ASC, "labelFr")));
        } else {
            page = repository
                    .findByLabelContainingIgnoreCaseAndVisible(partialLabel, visible,
                            PageRequest.of(0, limit, Sort.by(Direction.ASC, "label")));
        }
        return page
                .getContent().stream().map(t -> ToponymDTO
                        .build(t.getBusinessId(), t.getLabel(), t.getLabelFr(), null, t.getCopyright(),
                                t.getDescription(), t.isVisible(), t.getToponymMetadata())).collect(Collectors.toList());
    }


    /**
     * Generate a not visible toponym
     * @param user the user who has requested the toponym creation
     * @param project the project on which the toponym has been dropped
     * @return a {@link ToponymDTO}
     */
    public ToponymDTO generateNotVisibleToponym(String featureString, String user, String project) throws ModuleException, JsonProcessingException {
        // Count if user has reached the limit of toponyms to save per day
        OffsetDateTime startDayTime = OffsetDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT, ZoneOffset.UTC);
        int nbCreations = this.repository.countByToponymMetadataAuthorAndToponymMetadataCreationDateBetween(user, startDayTime, OffsetDateTime.now());

        if (nbCreations >= this.limitSave) {
            throw new MaxLimitPerDayException(String.format("User %s has reached the maximum number of toponyms to be saved in one day (max. %d).", user, this.limitSave));
        } else {
            // --- GEOMETRY PARSING ---
            Geometry<Position> geometry = parseGeometry(featureString);
            // --- TOPONYM GENERATION
            // define parameters
            OffsetDateTime currentDateTime = OffsetDateTime.now();
            String bid = String.format("Toponym_%s", OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            ToponymMetadata metadata = new ToponymMetadata(currentDateTime, currentDateTime.plusDays(this.defaultExpiration), user, project);
            return getToponymDTO(this.repository.save(new Toponym(bid, bid, bid, geometry, null, null, false, metadata)), sampling);
        }
    }

    /**
     * Utils method to get the {@link ToponymDTO} from a {@link Toponym}
     * @param t toponym
     * @param samplingMax maximum number of points to retrieve in geometry
     * @return {@link ToponymDTO}
     */
    public ToponymDTO getToponymDTO(Toponym t, int samplingMax) {
        return ToponymDTO.build(t.getBusinessId(), t.getLabel(), t.getLabelFr(),
                ToponymsIGeometryHelper.parseLatteGeometry(t.getGeometry(), samplingMax), t.getCopyright(),
                t.getDescription(), t.isVisible(), t.getToponymMetadata());
    }


    /**
     * Utils method to parse a geometry
     * @param featureString the feature in string format
     * @return the geometry with {@link Geometry} format
     * @throws ModuleException if a problem occurred during the parsing of the geometry
     * @throws JsonProcessingException if a problem occurred during the parsing of the geometry
     */
    private Geometry<Position> parseGeometry(String featureString) throws ModuleException, JsonProcessingException {
        // define mapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new GeolatteGeomModule());
        // parse geometry
        Feature<?, ?> feature = mapper.readValue(featureString, Feature.class);
        // if geometry could not be read
        if (feature.getGeometry() == null || feature.getGeometry().getGeometryType() == null) {
            throw new GeometryNotProcessedException("The geometry could not be processed. The toponym will not be saved. " +
                    "Check the format of the geojson feature");
        } else {
            Geometry<Position> geometry = (Geometry<Position>) feature.getGeometry();
            // refuse not handled geometry types
            GeometryType geometryType = geometry.getGeometryType();
            if (!geometryType.equals(GeometryType.POLYGON) && !geometryType.equals(GeometryType.MULTIPOLYGON)) {
                throw new GeometryNotHandledException(geometryType.toString());
            }
            // check geometry is not already present in the database
            if (this.repository.countByGeometry(geometry.toString()) > 0) {
                throw new EntityAlreadyExistsException("The geometry parsed already exists in the database. The toponym will not be saved.");
            }
            return geometry;
        }
    }

}
