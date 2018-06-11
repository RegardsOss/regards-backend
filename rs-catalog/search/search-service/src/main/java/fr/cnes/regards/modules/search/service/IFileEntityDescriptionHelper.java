package fr.cnes.regards.modules.search.service;

import java.io.IOException;

import feign.Response;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;

public interface IFileEntityDescriptionHelper {

    /**
     * If the user has access to the dataset, return the corresponding file
     *
     * @param datasetIpId
     * @param response
     * @return
     * @throws EntityOperationForbiddenException
     */
    Response getFile(UniformResourceName datasetIpId)
            throws EntityOperationForbiddenException, IOException, EntityNotFoundException;
}
