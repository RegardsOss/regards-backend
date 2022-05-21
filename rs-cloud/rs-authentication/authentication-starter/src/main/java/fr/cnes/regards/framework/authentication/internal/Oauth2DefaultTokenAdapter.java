package fr.cnes.regards.framework.authentication.internal;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import fr.cnes.regards.framework.gson.annotation.GsonTypeAdapterBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2RefreshToken;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;

/**
 * Customize {@link com.google.gson.Gson} to be able to respect Oauth2 Response format.
 * As we are in a package that is not scanned by spring application, it is already referenced in spring.factories
 *
 * @author Sylvain VISSIERE-GUERINET
 */
@GsonTypeAdapterBean(adapted = DefaultOAuth2AccessToken.class)
public class Oauth2DefaultTokenAdapter extends TypeAdapter<DefaultOAuth2AccessToken>
    implements ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(Oauth2DefaultTokenAdapter.class);

    private ApplicationContext applicationContext;

    private Gson gson;

    @Override
    public void write(JsonWriter out, DefaultOAuth2AccessToken token) throws IOException {
        out.beginObject();

        out.name(OAuth2AccessToken.ACCESS_TOKEN);
        out.value(token.getValue());

        out.name(OAuth2AccessToken.TOKEN_TYPE);
        out.value(token.getTokenType());

        OAuth2RefreshToken refreshToken = token.getRefreshToken();
        if (refreshToken != null) {
            out.name(OAuth2AccessToken.REFRESH_TOKEN);
            out.value(refreshToken.getValue());
        }
        Date expiration = token.getExpiration();
        if (expiration != null) {
            long now = System.currentTimeMillis();
            out.name(OAuth2AccessToken.EXPIRES_IN);
            out.value((expiration.getTime() - now) / 1000);
        }
        Set<String> scope = token.getScope();
        if (scope != null && !scope.isEmpty()) {
            StringJoiner scopes = new StringJoiner(" ");
            for (String s : scope) {
                Assert.hasLength(s, "Scopes cannot be null or empty. Got " + scope + "");
                scopes.add(s);
            }
            out.name(OAuth2AccessToken.SCOPE);
            out.value(scopes.toString().substring(0, scopes.length() - 1));
        }
        Map<String, Object> additionalInformation = token.getAdditionalInformation();
        for (Map.Entry<String, Object> entry : additionalInformation.entrySet()) {
            out.name(entry.getKey());
            out.jsonValue(gson.toJson(entry.getValue()));
        }
        out.endObject();
    }

    @Override
    public DefaultOAuth2AccessToken read(JsonReader jsonReader) throws IOException {
        OAuth2AccessToken token = DefaultOAuth2AccessToken.valueOf(gson.fromJson(jsonReader, LinkedHashMap.class));
        return (DefaultOAuth2AccessToken) token;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (this.gson == null) {
            //lets see if gson has been configured
            try {
                this.gson = applicationContext.getBean(Gson.class);
            } catch (NoSuchBeanDefinitionException e) {
                LOG.trace("Gson has not been initialized yet", e);
            }
        }
    }
}
