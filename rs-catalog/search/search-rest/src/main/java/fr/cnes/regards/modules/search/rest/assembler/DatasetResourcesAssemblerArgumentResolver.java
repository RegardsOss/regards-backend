package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link FacettedPagedResourcesAssembler} into Spring MVC
 * controller methods.
 *
 * @author Xavier-Alexandre Brochard
 */
public class DatasetResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

    private final IResourceService resourceService;

    private final DatasetLinkAdder datasetLinkAdder;

    /**
     * @param pResourceService
     * @param pDatasetLinkAdder
     */
    public DatasetResourcesAssemblerArgumentResolver(IResourceService pResourceService,
            DatasetLinkAdder pDatasetLinkAdder) {
        super();
        resourceService = pResourceService;
        datasetLinkAdder = pDatasetLinkAdder;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return DatasetResourcesAssembler.class.equals(parameter.getParameterType());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return new DatasetResourcesAssembler(datasetLinkAdder, resourceService);
    }

}
