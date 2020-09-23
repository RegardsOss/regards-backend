package fr.cnes.regards.modules.featureprovider.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.feature.dto.Feature;
import fr.cnes.regards.modules.feature.dto.FeatureReferenceCollection;
import fr.cnes.regards.modules.feature.dto.RequestInfo;
import fr.cnes.regards.modules.featureprovider.domain.FeatureExtractionRequest;
import fr.cnes.regards.modules.featureprovider.service.IFeatureExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController
@RequestMapping(FeatureExtractionController.EXTRACTION_PATH)
public class FeatureExtractionController implements IResourceController<RequestInfo<String>> {

    public static final String EXTRACTION_PATH = "/extraction";

    @Autowired
    private IFeatureExtractionService featureReferenceService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Create a list of {@link FeatureExtractionRequest} from a list of locationsstored in a {@link FeatureReferenceCollection}
     * and return a {@link RequestInfo} full of urns and occured errors
     * @param collection {@link FeatureExtractionRequest} it contain all {@link Feature} to handle
     * @return {@link RequestInfo}
     */
    @Operation(
            summary = "Publish locations collection to create features and return urns of granted and denied requests ids",
            description = "Publish locations collection to create features and return urns of granted and denied requests ids")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "A RequestInfo") })
    @ResourceAccess(
            description = "Publish locations collection to create features and return urns of granted and denied requests ids")
    @RequestMapping(method = RequestMethod.POST, consumes = GeoJsonMediaType.APPLICATION_GEOJSON_VALUE)
    public ResponseEntity<EntityModel<RequestInfo<String>>> createFeaturesFromReferences(
            @Parameter(description = "Contain all Features to handle") @Valid @RequestBody
                    FeatureReferenceCollection collection) {

        RequestInfo<String> info = this.featureReferenceService.registerRequests(collection);
        return new ResponseEntity<>(toResource(info), computeStatus(info));
    }

    /**
     * Compute {@link HttpStatus} according to information return by the service
     */
    private HttpStatus computeStatus(RequestInfo<?> info) {
        boolean hasGranted = !info.getGranted().isEmpty();
        boolean hasDenied = !info.getDenied().isEmpty();

        HttpStatus status;
        if (hasGranted && hasDenied) {
            status = HttpStatus.PARTIAL_CONTENT; // 206
        } else if (hasDenied) {
            status = HttpStatus.UNPROCESSABLE_ENTITY; // 422
        } else {
            status = HttpStatus.CREATED; // 201
        }
        return status;
    }

    @Override
    public EntityModel<RequestInfo<String>> toResource(RequestInfo<String> element, Object... extras) {
        return resourceService.toResource(element);
    }
}
