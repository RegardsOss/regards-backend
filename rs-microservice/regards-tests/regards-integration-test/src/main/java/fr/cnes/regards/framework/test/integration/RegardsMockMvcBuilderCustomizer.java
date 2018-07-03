package fr.cnes.regards.framework.test.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Allows to customize MockMvc bean. <br/>
 * Customization: <br/>
 * <ul>
 * <li>usage of markdown to generate API documentation instead of Asciidoctor</li>
 * </ul>
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
@ConditionalOnBean(value = { RestDocumentationContextProvider.class })
public class RegardsMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

    /**
     * {@link RestDocumentationContextProvider} instance
     */
    @Autowired
    private RestDocumentationContextProvider restDocumentation;

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation).snippets()
                .withDefaults(new RegardsHttpRequestSnippet(), new RegardsHttpResponseSnippet(),
                              new RegardsRequestBodySnippet()).withTemplateFormat(TemplateFormats.markdown()));
    }
}
