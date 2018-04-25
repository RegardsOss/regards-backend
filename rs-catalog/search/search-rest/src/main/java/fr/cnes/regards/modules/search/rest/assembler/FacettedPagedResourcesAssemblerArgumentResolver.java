package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.core.MethodParameter;
import org.springframework.data.web.HateoasPageableHandlerMethodArgumentResolver;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PagedResourcesAssemblerArgumentResolver;
import org.springframework.hateoas.MethodLinkBuilderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import fr.cnes.regards.modules.indexer.domain.IIndexable;

/**
 * {@link HandlerMethodArgumentResolver} to allow injection of {@link FacettedPagedResourcesAssembler} into Spring MVC
 * controller methods.
 *
 * @author Xavier-Alexandre Brochard
 */
public class FacettedPagedResourcesAssemblerArgumentResolver extends PagedResourcesAssemblerArgumentResolver {

    /**
     * @param pResolver
     * @param pLinkBuilderFactory
     */
    public FacettedPagedResourcesAssemblerArgumentResolver(HateoasPageableHandlerMethodArgumentResolver pResolver,
            MethodLinkBuilderFactory<?> pLinkBuilderFactory) {
        super(pResolver, pLinkBuilderFactory);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#supportsParameter(org.springframework.core.MethodParameter)
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return FacettedPagedResourcesAssembler.class.equals(parameter.getParameterType());
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.web.method.support.HandlerMethodArgumentResolver#resolveArgument(org.springframework.core.MethodParameter, org.springframework.web.method.support.ModelAndViewContainer, org.springframework.web.context.request.NativeWebRequest, org.springframework.web.bind.support.WebDataBinderFactory)
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Object resolveArgument = super.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        return new FacettedPagedResourcesAssembler<IIndexable>((PagedResourcesAssembler) resolveArgument);
    }

}
