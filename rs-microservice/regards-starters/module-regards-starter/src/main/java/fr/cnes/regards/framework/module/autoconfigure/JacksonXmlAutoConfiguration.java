package fr.cnes.regards.framework.module.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * We have to trick spring so we can access all of jackson xml converters to add jaxb annotation support
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@Component
@ConditionalOnProperty(name = "regards.xml.jackson.as.jaxb", havingValue = "true")
public class JacksonXmlAutoConfiguration {

    @Autowired
    private List<MappingJackson2XmlHttpMessageConverter> jacksonConverters;

    @EventListener
    public void handle(ApplicationReadyEvent event) {
        JaxbAnnotationModule jaxbModule = new JaxbAnnotationModule();
        for (MappingJackson2XmlHttpMessageConverter jacksonConverter : jacksonConverters) {
            ObjectMapper om = jacksonConverter.getObjectMapper();
            om.setSerializationInclusion(Include.NON_NULL);
            if (om instanceof XmlMapper) {
                ((XmlMapper) om).setDefaultUseWrapper(false);
            }
            jacksonConverter.getObjectMapper().registerModule(jaxbModule);
        }
    }

}
