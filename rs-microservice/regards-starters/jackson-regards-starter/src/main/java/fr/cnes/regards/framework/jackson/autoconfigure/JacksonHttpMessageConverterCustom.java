package fr.cnes.regards.framework.jackson.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Converter to deserialize/serialize http requests/responses in JSON with Jackson
 *
 * @author tmichaud
 */
@Component
public class JacksonHttpMessageConverterCustom extends AbstractJackson2HttpMessageConverter {

    public JacksonHttpMessageConverterCustom(ObjectMapper objectMapper) {
        super(objectMapper, MediaType.APPLICATION_JSON);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType);
    }

    @Override
    protected boolean canWrite(MediaType mediaType) {
        return MediaType.APPLICATION_JSON.includes(mediaType);
    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage) throws IOException {
        return super.readInternal(clazz, inputMessage);
    }

    @Override
    protected void writeInternal(Object object, HttpOutputMessage outputMessage) throws IOException {
        super.writeInternal(object, outputMessage);
    }
}
