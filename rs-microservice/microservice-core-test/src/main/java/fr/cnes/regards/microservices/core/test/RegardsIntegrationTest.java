/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * Base class to realize integration tests using JWT and MockMvc
 *
 * @author svissier
 *
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class RegardsIntegrationTest {

    private Logger logger;

    @Autowired
    private MockMvc mvc_;

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public void performGet(String urlTemplate, String authentificationToken, List<ResultMatcher> matchers,
            String errorMessage, Object... pUrlVariables) {
        try {
            ResultActions request = mvc_.perform(get(urlTemplate, pUrlVariables)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken));
            for (ResultMatcher matcher : matchers) {
                request = request.andExpect(matcher);
            }
        }
        catch (Exception e) {
            logger.error(errorMessage, e);
            Assert.fail(errorMessage);
        }
    }

    public void performPost(String urlTemplate, String authentificationToken, Object content,
            List<ResultMatcher> matchers, String errorMessage, Object... pUrlVariables) {
        try {
            ResultActions request = mvc_.perform(post(urlTemplate, pUrlVariables).with(csrf()).content(json(content))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
            for (ResultMatcher matcher : matchers) {
                request = request.andExpect(matcher);
            }
        }
        catch (IOException e) {
            String message = "Cannot (de)serialize model";
            logger.error(message, e);
            Assert.fail(message);
        }
        catch (Exception e) {
            logger.error(errorMessage, e);
            Assert.fail(errorMessage);
        }
    }

    public void performPut(String urlTemplate, String authentificationToken, Object content,
            List<ResultMatcher> matchers, String errorMessage, Object... pUrlVariables) {
        try {
            ResultActions request = mvc_.perform(put(urlTemplate, pUrlVariables).with(csrf()).content(json(content))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));
            for (ResultMatcher matcher : matchers) {
                request = request.andExpect(matcher);
            }
        }
        catch (IOException e) {
            String message = "Cannot (de)serialize model";
            logger.error(message, e);
            Assert.fail(message);
        }
        catch (Exception e) {
            logger.error(errorMessage, e);
            Assert.fail(errorMessage);
        }
    }

    public void performDelete(String urlTemplate, String authentificationToken, List<ResultMatcher> matchers,
            String errorMessage, Object... pUrlVariables) {
        try {
            ResultActions request = mvc_.perform(delete(urlTemplate, pUrlVariables).with(csrf())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + authentificationToken));
            for (ResultMatcher matcher : matchers) {
                request = request.andExpect(matcher);
            }
        }
        catch (Exception e) {
            logger.error(errorMessage, e);
            Assert.fail(errorMessage);
        }
    }

    private String json(Object o) throws IOException {
        if (o instanceof String) {
            return (String) o;
        }
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(o);
    }
}
