package fr.cnes.regards.modules.search.service;

import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.EntityOperationForbiddenException;
import fr.cnes.regards.modules.entities.urn.UniformResourceName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
    ResponseEntity<StreamingResponseBody> getFile(UniformResourceName datasetIpId, HttpServletResponse response) throws EntityOperationForbiddenException, IOException, EntityNotFoundException;
}
