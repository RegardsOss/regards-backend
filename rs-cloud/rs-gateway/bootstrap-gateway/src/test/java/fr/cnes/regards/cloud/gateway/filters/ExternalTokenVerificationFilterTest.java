package fr.cnes.regards.cloud.gateway.filters;

import fr.cnes.regards.cloud.gateway.authentication.ExternalAuthenticationVerifier;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
import fr.cnes.regards.modules.authentication.domain.data.Authentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.UUID;

import static fr.cnes.regards.cloud.gateway.filters.FilterConstants.BEARER;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExternalTokenVerificationFilterTest {

    private final static String TENANT = "DEFAULT";

    private final static String DUMMY_URL = "http://dummyUrl.com";

    @InjectMocks
    @Spy
    private ExternalTokenVerificationFilter tokenFilter;

    @Mock
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Mock
    private JWTService jwtService;

    @Mock
    private ExternalAuthenticationVerifier externalAuthenticationVerifier;

    @Mock
    private GatewayFilterChain filterChain;

    private ServerWebExchange exchange;

    private final ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);

    private AutoCloseable closeable;

    @BeforeEach
    void initService() {
        closeable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void closeService() throws Exception {
        closeable.close();
    }

    @Test
    void valid_regards_token_is_validated_not_verified_against_authentication_service() throws JwtException {
        // -- GIVEN --
        String token = UUID.randomUUID().toString();
        JWTAuthentication authentication = mock(JWTAuthentication.class);
        when(authentication.getTenant()).thenReturn(TENANT);
        when(authentication.getJwt()).thenReturn(token);
        doNothing().when(authentication).setTenant(any());
        when(jwtService.parseToken(any())).thenReturn(authentication);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpConstants.AUTHORIZATION, BEARER + " " + token);
        requestHeaders.add(HttpConstants.SCOPE, TENANT);
        MockServerHttpRequest request = MockServerHttpRequest.get(DUMMY_URL).headers(requestHeaders).build();
        exchange = MockServerWebExchange.from(request);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        // -- WHEN --
        tokenFilter.filter(exchange, filterChain).block();

        // -- THEN --
        ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
        assertFalse(tokenFilter.getInvalidCache().asMap().containsKey(token));
        assertTrue(tokenFilter.getValidCache().asMap().containsKey(token));
        assertEquals(token, tokenFilter.getValidCache().getIfPresent(token));
        assertTrue(modifiedRequest.getHeaders().containsKey(HttpConstants.AUTHORIZATION));
        assertEquals(BEARER + " " + token, modifiedRequest.getHeaders().getFirst(HttpConstants.AUTHORIZATION));

        verify(externalAuthenticationVerifier, never()).verifyAndAuthenticate(anyString(), anyString());

    }

    @Test
    void invalid_regards_token_is_verified_against_authentication_service_and_cached_as_invalid_if_verification_fails()
        throws JwtException {
        // -- GIVEN --
        String token = UUID.randomUUID().toString();
        String authenticationToken = UUID.randomUUID().toString();

        JWTAuthentication authentication = mock(JWTAuthentication.class);
        when(authentication.getTenant()).thenReturn(TENANT);
        when(authentication.getJwt()).thenReturn(authenticationToken);
        doNothing().when(authentication).setTenant(any());
        JwtException expected = new JwtException("Expected");
        when(jwtService.parseToken(any())).thenThrow(expected);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpConstants.AUTHORIZATION, BEARER + " " + token);
        requestHeaders.add(HttpConstants.SCOPE, TENANT);
        MockServerHttpRequest request = MockServerHttpRequest.get(DUMMY_URL).headers(requestHeaders).build();
        exchange = MockServerWebExchange.from(request);

        RuntimeException err = new RuntimeException("Expected test exception");
        when(externalAuthenticationVerifier.verifyAndAuthenticate(anyString(),
                                                                  anyString())).thenReturn(Mono.error(err));

        // -- WHEN --
        StepVerifier.create(tokenFilter.filter(exchange, filterChain)).verifyComplete();

        // -- THEN --
        assertFalse(tokenFilter.getValidCache().asMap().containsKey(token));
        assertTrue(tokenFilter.getInvalidCache().asMap().containsKey(token));
    }

    @Test
    void invalid_regards_token_is_verified_against_authentication_service_and_cached_as_valid_if_verification_succeeds()
        throws JwtException {
        // -- GIVEN --
        String token = UUID.randomUUID().toString();

        JWTAuthentication authentication = mock(JWTAuthentication.class);
        when(authentication.getTenant()).thenReturn(TENANT);
        when(authentication.getJwt()).thenReturn(token);
        doNothing().when(authentication).setTenant(any());
        JwtException expected = new JwtException("Expected");
        when(jwtService.parseToken(any())).thenThrow(expected);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpConstants.AUTHORIZATION, BEARER + " " + token);
        requestHeaders.add(HttpConstants.SCOPE, TENANT);
        MockServerHttpRequest request = MockServerHttpRequest.get(DUMMY_URL).headers(requestHeaders).build();
        exchange = MockServerWebExchange.from(request);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        String newToken = UUID.randomUUID().toString();
        when(externalAuthenticationVerifier.verifyAndAuthenticate(anyString(),
                                                                  anyString())).thenReturn(Mono.just(new Authentication(
            TENANT,
            "example@test.com",
            null,
            "rs-authentication",
            newToken,
            OffsetDateTime.now())));

        // -- WHEN --
        StepVerifier.create(tokenFilter.filter(exchange, filterChain)).verifyComplete();

        // -- THEN --
        ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
        assertFalse(tokenFilter.getInvalidCache().asMap().containsKey(token));
        assertTrue(tokenFilter.getValidCache().asMap().containsKey(token));
        assertEquals(newToken, tokenFilter.getValidCache().getIfPresent(token));
        assertTrue(modifiedRequest.getHeaders().containsKey(HttpConstants.AUTHORIZATION));
        assertEquals(BEARER + " " + newToken, modifiedRequest.getHeaders().getFirst(HttpConstants.AUTHORIZATION));

        verify(externalAuthenticationVerifier).verifyAndAuthenticate(authentication.getJwt(),
                                                                     authentication.getTenant());

    }

    @Test
    void cached_invalid_token_is_not_checked_at_all() {
        // -- GIVEN --
        String token = UUID.randomUUID().toString();
        String mappedToken = UUID.randomUUID().toString();

        tokenFilter.getInvalidCache().put(token, mappedToken);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpConstants.AUTHORIZATION, BEARER + " " + token);
        requestHeaders.add(HttpConstants.SCOPE, TENANT);
        MockServerHttpRequest request = MockServerHttpRequest.get(DUMMY_URL).headers(requestHeaders).build();
        exchange = MockServerWebExchange.from(request);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        // -- WHEN --
        StepVerifier.create(tokenFilter.filter(exchange, filterChain)).verifyComplete();

        // -- THEN --
        verifyNoInteractions(jwtService, runtimeTenantResolver, externalAuthenticationVerifier);
        verify(externalAuthenticationVerifier, times(0)).verifyAndAuthenticate(anyString(), anyString());

    }

    @Test
    void cached_valid_token_is_passed_downstream() {
        // -- GIVEN --
        String token = UUID.randomUUID().toString();
        String mappedToken = UUID.randomUUID().toString();

        tokenFilter.getValidCache().put(token, mappedToken);

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add(HttpConstants.AUTHORIZATION, BEARER + " " + token);
        requestHeaders.add(HttpConstants.SCOPE, TENANT);
        MockServerHttpRequest request = MockServerHttpRequest.get(DUMMY_URL).headers(requestHeaders).build();
        exchange = MockServerWebExchange.from(request);
        filterChain = mock(GatewayFilterChain.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        // -- WHEN --
        StepVerifier.create(tokenFilter.filter(exchange, filterChain)).verifyComplete();

        // -- THEN --
        ServerHttpRequest modifiedRequest = captor.getValue().getRequest();
        assertTrue(modifiedRequest.getHeaders().containsKey(HttpConstants.AUTHORIZATION));
        assertEquals(BEARER + " " + mappedToken, modifiedRequest.getHeaders().getFirst(HttpConstants.AUTHORIZATION));
        verifyNoInteractions(jwtService, runtimeTenantResolver, externalAuthenticationVerifier);
        verify(externalAuthenticationVerifier, times(0)).verifyAndAuthenticate(anyString(), anyString());
    }

    @Test
    void verify_fail_when_client_fails() {
        HttpClientErrorException:
        {
            WebClientResponseException expected = new WebClientResponseException(HttpStatus.BAD_REQUEST.value(),
                                                                                 "Bad request",
                                                                                 null,
                                                                                 null,
                                                                                 null);
            when(externalAuthenticationVerifier.verifyAndAuthenticate(anyString(), anyString())).thenReturn(Mono.error(
                expected));
            StepVerifier.create(externalAuthenticationVerifier.verifyAndAuthenticate("plop", "plop"))
                        .verifyError(WebClientResponseException.class);
        }

        HttpServerErrorException:
        {
            WebClientResponseException expected = new WebClientResponseException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                                                                                 "Service not available",
                                                                                 null,
                                                                                 null,
                                                                                 null);
            when(externalAuthenticationVerifier.verifyAndAuthenticate(anyString(), anyString())).thenReturn(Mono.error(
                expected));
            StepVerifier.create(externalAuthenticationVerifier.verifyAndAuthenticate("plop", "plop"))
                        .verifyError(WebClientResponseException.class);
        }
    }
}
