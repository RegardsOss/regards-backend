package fr.cnes.regards.framework.jackson.autoconfigure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

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
    protected boolean supports(Class<?> clazz) {
        // Do not support byte[], because byte[] serialization does a base64 encoding with jackson,
        // and we want to keep the default behavior of the Spring framework (see ByteArrayHttpMessageConverter).
        // This means that all types except byte[] are serialized by jackson.
        return byte[].class != clazz;
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return supports(clazz) && MediaType.APPLICATION_JSON.includes(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return supports(clazz) && MediaType.APPLICATION_JSON.includes(mediaType);
    }
}
