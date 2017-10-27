package fr.cnes.regards.modules.storage.rest;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.plugin.datastorage.PluginStorageInfo;
import fr.cnes.regards.modules.storage.service.IDataStorageService;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@RestController(StorageMonitoringController.PATH)
public class StorageMonitoringController implements IResourceController<PluginStorageInfo>{

    public static final String PATH = "/storages/monitoring";

    @Autowired
    private IDataStorageService dataStorageService;

    @Autowired
    private IResourceService resourceService;

    @RequestMapping(value = PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of all active data storage monitoring information")
    public ResponseEntity<List<Resource<PluginStorageInfo>>> retrieveMonitoringInfos()
            throws ModuleException, IOException {
        return new ResponseEntity<>(toResources(dataStorageService.getMonitoringInfos()), HttpStatus.OK);
    }

    @Override
    public Resource<PluginStorageInfo> toResource(PluginStorageInfo pElement, Object... pExtras) {
        Resource<PluginStorageInfo> resource = new Resource<>(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveMonitoringInfos", LinkRels.LIST);
        return resource;
    }
}
