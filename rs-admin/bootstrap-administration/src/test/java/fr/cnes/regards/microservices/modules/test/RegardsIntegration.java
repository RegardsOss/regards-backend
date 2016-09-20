/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.modules.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.util.Arrays;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.util.Assert;

/**
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class RegardsIntegration {

    private HttpMessageConverter mappingJackson2HttpMessageConverter;

    @Autowired
    private MockMvc mvc_;

    @Autowired
    void setConverters(HttpMessageConverter<?>[] converters) {

        mappingJackson2HttpMessageConverter = Arrays.asList(converters).stream()
                .filter(hmc -> hmc instanceof MappingJackson2HttpMessageConverter).findAny().get();

        Assert.notNull(mappingJackson2HttpMessageConverter, "the JSON message converter must not be null");
    }

    public void performGet(String urlTemplate, String authentificationToken, ResultMatcher matcher,
            Object... pUrlVariables) {
        try {
            mvc_.perform(get(urlTemplate, pUrlVariables).header(HttpHeaders.AUTHORIZATION,
                                                                "Bearer " + authentificationToken))
                    .andExpect(matcher);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void performPost(String urlTemplate, String authentificationToken, Object content, ResultMatcher matcher,
            Object... pUrlVariables) {
        try {
            mvc_.perform(post(urlTemplate, pUrlVariables).content(json(content))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken)).andExpect(matcher);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void performPut(String urlTemplate, String authentificationToken, Object content, ResultMatcher matcher,
            Object... pUrlVariables) {
        try {
            mvc_.perform(put(urlTemplate, pUrlVariables).content(json(content))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken)).andExpect(matcher);
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void performDelete(String urlTemplate, String authentificationToken, ResultMatcher matcher,
            Object... pUrlVariables) {
        try {
            mvc_.perform(delete(urlTemplate, pUrlVariables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken)).andExpect(matcher);
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private String json(Object o) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        mappingJackson2HttpMessageConverter.write(o, MediaType.APPLICATION_JSON, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }
}
