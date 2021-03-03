package fr.cnes.regards.modules.authentication.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.authentication.domain.service.IServiceProviderCrudService;
import fr.cnes.regards.modules.authentication.rest.dto.ServiceProviderPublicDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Function;

@RestController
public class ServiceProviderPublicController implements IResourceController<ServiceProviderPublicDto> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceProviderPublicController.class);

    public static final String PATH_SERVICE_PROVIDER_PUBLIC = "/serviceproviders/public";

    @Autowired
    private IServiceProviderCrudService serviceProviderCrud;

    @Autowired
    private IResourceService resourceService;

    @ResponseBody
    @GetMapping(value = PATH_SERVICE_PROVIDER_PUBLIC)
    @ResourceAccess(description = "Retrieves the list of service providers.", role = DefaultRole.PUBLIC)
    public ResponseEntity<PagedModel<EntityModel<ServiceProviderPublicDto>>> getServiceProvidersPublic(
        @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        PagedResourcesAssembler<ServiceProviderPublicDto> assembler
    ) throws ModuleException {
        return serviceProviderCrud.findAll(pageable)
            .map(page -> page.map(ServiceProviderPublicDto::new))
            .map(page -> new ResponseEntity<>(toPagedResources(page, assembler), HttpStatus.OK))
            .getOrElseThrow((Function<Throwable, ModuleException>) ModuleException::new);
    }

    @Override
    public EntityModel<ServiceProviderPublicDto> toResource(final ServiceProviderPublicDto element, final Object... extras) {
        EntityModel<ServiceProviderPublicDto> resource = resourceService.toResource(element);
        if ((element != null)) {
            resource = resourceService.toResource(element);
            resourceService.addLink(resource, this.getClass(), "getServiceProvidersPublic", LinkRels.LIST,
                MethodParamFactory.build(Pageable.class),
                MethodParamFactory.build(PagedResourcesAssembler.class));
        }
        return resource;
    }
}
