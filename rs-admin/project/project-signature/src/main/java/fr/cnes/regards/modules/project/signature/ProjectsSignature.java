/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.project.signature;

import java.util.List;

import javax.naming.OperationNotSupportedException;
import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.project.domain.Project;

public interface ProjectsSignature {

    @RequestMapping(value = "/projects", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    HttpEntity<List<Resource<Project>>> retrieveProjectList();

    @RequestMapping(value = "/projects", method = RequestMethod.POST, consumes = "application/json", produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<Project>> createProject(@Valid @RequestBody Project newProject) throws AlreadyExistingException;

    @RequestMapping(method = RequestMethod.GET, value = "/projects/{project_id}", produces = "application/json")
    @ResponseBody
    HttpEntity<Resource<Project>> retrieveProject(@PathVariable("project_id") String projectId);

    @RequestMapping(method = RequestMethod.PUT, value = "/projects/{project_id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    HttpEntity<Void> modifyProject(@PathVariable("project_id") String projectId, @RequestBody Project projectUpdated)
            throws OperationNotSupportedException;

    @RequestMapping(method = RequestMethod.DELETE, value = "/projects/{project_id}", produces = "application/json")
    @ResponseBody
    HttpEntity<Void> deleteProject(@PathVariable("project_id") String projectId);
}
