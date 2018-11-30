package fr.cnes.regards.framework.test.integration;

import org.springframework.boot.test.autoconfigure.restdocs.RestDocsMockMvcConfigurationCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentationConfigurer;
import org.springframework.restdocs.templates.TemplateFormats;

/**
 * Allows to customize MockMvc bean. <br/>
 * Customization: <br/>
 * - usage of markdown to generate API documentation instead of Asciidoctor<br>
 * Best see following doc than spring-restdocs in case of API change
 * @see 'https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-testing-spring-boot-applications-testing-autoconfigured-rest-docs-mock-mvc'
 * @author Sylvain VISSIERE-GUERINET
 */
@TestConfiguration
public class RegardsMockMvcBuilderCustomizer implements RestDocsMockMvcConfigurationCustomizer {

    /**
     * Customize MockMvcBean to provide :<br>
     * - our HttpRequest snippet,<br>
     * - our HttpResponse snippet,<br>
     * - our Request body snippet.<br>
     * And to force using Markdown instead of asciidoctor
     */
    @Override
    public void customize(MockMvcRestDocumentationConfigurer configurer) {
        configurer.snippets().withDefaults(new RegardsHttpRequestSnippet(), new RegardsHttpResponseSnippet(),
                                           new RegardsRequestBodySnippet())
                .withTemplateFormat(TemplateFormats.markdown());
    }
}
