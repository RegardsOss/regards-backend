/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.configuration.domain.LinkUIPluginsDatasets;
import fr.cnes.regards.modules.configuration.service.link.ILinkUIPluginsDatasetsService;

/**
 *
 * Class LinkUIPluginsDatasetsController
 *
 * Rest controller to link a dataset to one or many UIPluginConfiguration
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
@RestController
@RequestMapping(path = LinkUIPluginsDatasetsController.REQUEST_MAPPING_ROOT)
public class LinkUIPluginsDatasetsController {

    public static final String REQUEST_MAPPING_ROOT = "/linkuiplugindataset/{datasetId}";

    @Autowired
    private ILinkUIPluginsDatasetsService linkService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<LinkUIPluginsDatasets> retrieveLink(@PathVariable("datasetId") final String pDatasetId)
            throws EntityNotFoundException {
        final LinkUIPluginsDatasets link = linkService.retrieveLink(pDatasetId);
        return new ResponseEntity<>(link, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "endpoint allowing to modify which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<LinkUIPluginsDatasets> updateLink(@PathVariable("datasetId") final String pDatasetId,
            @RequestBody final LinkUIPluginsDatasets pUpdatedLink) throws EntityException {
        final LinkUIPluginsDatasets link = linkService.updateLink(pDatasetId, pUpdatedLink);
        return new ResponseEntity<>(link, HttpStatus.OK);
    }

}
