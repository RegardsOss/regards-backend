package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Léo Mieulet
 */
@RestController
@RequestMapping(StorageDataFileController.TYPE_MAPPING)
public class StorageDataFileController implements IResourceController<StorageDataFile> {

    public static final String TYPE_MAPPING = "/data-file";

    public static final String AIP_PATH = "/aip/{id}";

    @Autowired
    private IAIPService aipService;

    /**
     * Service handling hypermedia resources
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve the aip files metadata associated to an aip, represented by its ip id
     * @return aip files metadata associated to the aip
     */
    @RequestMapping(value = AIP_PATH, method = RequestMethod.GET)
    @ResponseBody
    @ResourceAccess(description = "send the list of files data files of a specified aip")
    public ResponseEntity<List<Resource<StorageDataFile>>> retrieveAIPFiles(@PathVariable("id") @Valid String pIpId)
            throws ModuleException {
        Set<StorageDataFile> files = aipService.retrieveAIPDataFiles(UniformResourceName.fromString(pIpId));
        return new ResponseEntity<>(toResources(files), HttpStatus.OK);
    }

    @Override
    public Resource<StorageDataFile> toResource(StorageDataFile element, Object... extras) {
        return new Resource<>(element);
    }

    @Override
    public List<Resource<StorageDataFile>> toResources(Collection<StorageDataFile> elements, Object... extras) {
        // Adapt the result to match front expectations
        List<Resource<StorageDataFile>> result = new ArrayList<>(elements.size());
        elements.forEach(f -> result.add(new Resource<>(f)));
        return result;
    }
}
