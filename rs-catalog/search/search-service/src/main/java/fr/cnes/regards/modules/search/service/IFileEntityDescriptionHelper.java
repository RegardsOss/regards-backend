package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

public interface IFileEntityDescriptionHelper {

    /**
     * If the user has access to the dataset, return the corresponding file
     *
     * @param datasetIpId
     * @return
     * @throws EntityOperationForbiddenException
     */
    ResponseEntity<InputStreamResource> getFile(UniformResourceName datasetIpId) throws EntityOperationForbiddenException, IOException, EntityNotFoundException;
}
