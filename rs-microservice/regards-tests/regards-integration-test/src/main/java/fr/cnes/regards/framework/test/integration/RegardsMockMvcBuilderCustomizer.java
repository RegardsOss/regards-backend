package fr.cnes.regards.framework.test.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
public class RegardsMockMvcBuilderCustomizer implements MockMvcBuilderCustomizer {

    @Autowired
    private RestDocumentationContextProvider restDocumentation;

    @Override
    public void customize(ConfigurableMockMvcBuilder<?> builder) {
        builder.apply(MockMvcRestDocumentation.documentationConfiguration(this.restDocumentation)
                              .snippets().withTemplateFormat(TemplateFormats.markdown()));
    }
}
