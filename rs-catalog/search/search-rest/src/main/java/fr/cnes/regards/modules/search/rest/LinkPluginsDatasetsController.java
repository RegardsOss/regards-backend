/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.service.link.LinkPluginsDatasetsService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(path = LinkPluginsDatasetsController.PATH_LINK)
public class LinkPluginsDatasetsController {

    public static final String PATH_LINK = "/linkplugindataset/{datasetId}";

    @Autowired
    private LinkPluginsDatasetsService linkService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<LinkPluginsDatasets> retrieveLink(@PathVariable("pDatasetId") Long pDatasetId)
            throws EntityNotFoundException {
        LinkPluginsDatasets link = linkService.retrieveLink(pDatasetId);
        return new ResponseEntity<>(link, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "endpoint allowing to modify which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<LinkPluginsDatasets> updateLink(@PathVariable("pDatasetId") Long pDatasetId,
            @RequestBody LinkPluginsDatasets pUpdatedLink) throws EntityNotFoundException {
        LinkPluginsDatasets link = linkService.updateLink(pDatasetId, pUpdatedLink);
        return new ResponseEntity<>(link, HttpStatus.OK);
    }

}
