package fr.cnes.regards.modules.search.service;

import feign.Response;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface IFileEntityDescriptionHelper {

    /**
     * If the user has access to the dataset, return the corresponding file
     *
     * @param datasetIpId
     * @param response
     * @return
     * @throws EntityOperationForbiddenException
     */
    Response getFile(UniformResourceName datasetIpId, HttpServletResponse response) throws EntityOperationForbiddenException, IOException, EntityNotFoundException;
}
