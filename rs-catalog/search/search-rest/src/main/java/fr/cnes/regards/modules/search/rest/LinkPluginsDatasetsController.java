/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import fr.cnes.regards.framework.hateoas.IResourceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.search.domain.LinkPluginsDatasets;
import fr.cnes.regards.modules.search.service.link.ILinkPluginsDatasetsService;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RestController
@RequestMapping(path = LinkPluginsDatasetsController.PATH_LINK)
public class LinkPluginsDatasetsController {

    public static final String PATH_LINK = "/linkplugindataset/{datasetId}";

    @Autowired
    private ILinkPluginsDatasetsService linkService;


    /**
     * The resource service. Autowired by Spring.
     */
    @Autowired
    private IResourceService resourceService;

    @RequestMapping(method = RequestMethod.GET)
    @ResourceAccess(description = "endpoint allowing to retrieve which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<Resource<LinkPluginsDatasets>> retrieveLink(@PathVariable("datasetId") final String pDatasetId)
            throws EntityNotFoundException {
        final LinkPluginsDatasets link = linkService.retrieveLink(pDatasetId);
        Resource<LinkPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    @ResourceAccess(description = "endpoint allowing to modify which plugins are to be applied to a given dataset")
    @ResponseBody
    public ResponseEntity<Resource<LinkPluginsDatasets>> updateLink(@PathVariable("datasetId") final String pDatasetId,
            @RequestBody final LinkPluginsDatasets pUpdatedLink) throws EntityException {
        final LinkPluginsDatasets link = linkService.updateLink(pDatasetId, pUpdatedLink);
        Resource<LinkPluginsDatasets> resource = resourceService.toResource(link);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

}
