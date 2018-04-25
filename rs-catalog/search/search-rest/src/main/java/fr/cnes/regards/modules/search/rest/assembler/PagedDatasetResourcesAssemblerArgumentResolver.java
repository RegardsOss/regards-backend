package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.core.MethodParameter;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import fr.cnes.regards.modules.search.rest.assembler.link.DatasetLinkAdder;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link PagedDatasetResourcesAssembler} into Spring MVC
 * controller methods.
 *
 * @author Xavier-Alexandre Brochard
 */
public class PagedDatasetResourcesAssemblerArgumentResolver implements HandlerMethodArgumentResolver {

    private final DatasetLinkAdder datasetLinkAdder;

    private final HateoasPageableHandlerMethodArgumentResolver resolver;

    /**
     * @param pDatasetLinkAdder
     * @param pResolver
     */
    public PagedDatasetResourcesAssemblerArgumentResolver(DatasetLinkAdder pDatasetLinkAdder,
            HateoasPageableHandlerMethodArgumentResolver pResolver) {
        super();
        datasetLinkAdder = pDatasetLinkAdder;
        resolver = pResolver;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return PagedDatasetResourcesAssembler.class.equals(parameter.getParameterType());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return new PagedDatasetResourcesAssembler(resolver, datasetLinkAdder);
    }

}
