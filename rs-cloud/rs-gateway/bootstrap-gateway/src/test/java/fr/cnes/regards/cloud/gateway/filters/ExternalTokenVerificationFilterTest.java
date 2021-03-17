package fr.cnes.regards.cloud.gateway.filters;

//import com.netflix.zuul.context.RequestContext;
//import feign.FeignException;
//import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
//import fr.cnes.regards.framework.security.utils.HttpConstants;
//import fr.cnes.regards.framework.security.utils.jwt.JWTAuthentication;
//import fr.cnes.regards.framework.security.utils.jwt.JWTService;
//import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;
//import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationServiceException;
//import org.springframework.security.authentication.InsufficientAuthenticationException;
//import org.springframework.security.authentication.InternalAuthenticationServiceException;
//import org.springframework.web.client.HttpClientErrorException;
//import org.springframework.web.client.HttpServerErrorException;
//
//import javax.servlet.http.HttpServletRequest;
//import java.util.HashMap;
//import java.util.UUID;
//
//import static fr.cnes.regards.framework.security.utils.HttpConstants.BEARER;
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.junit.Assert.*;
//import static org.mockito.Mockito.*;

public class ExternalTokenVerificationFilterTest {

//    public static final String TENANT = "DEFAULT";
//
//    @Mock
//    private IRuntimeTenantResolver runtimeTenantResolver;
//
//    @Mock
//    private JWTService jwtService;
//
//    @Mock
//    private IExternalAuthenticationClient externalAuthenticationClient;
//
//    private ExternalTokenVerificationFilter filter;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        filter = spy(new ExternalTokenVerificationFilter(jwtService, runtimeTenantResolver, externalAuthenticationClient));
//    }
//
//    @Test
//    public void valid_regards_token_is_validated_not_verified_against_authentication_service() {
//        try {
//            String token = UUID.randomUUID().toString();
//
//            JWTAuthentication authentication = mock(JWTAuthentication.class);
//            when(authentication.getTenant()).thenReturn(TENANT);
//            when(authentication.getJwt()).thenReturn(token);
//            doNothing().when(authentication).setTenant(any());
//            when(jwtService.parseToken(any())).thenReturn(authentication);
//
//            RequestContext ctx = mock(RequestContext.class);
//            HashMap<String, String> zuulRequestHeaders = new HashMap<>();
//            when(ctx.getZuulRequestHeaders()).thenReturn(zuulRequestHeaders);
//
//            HttpServletRequest request = mock(HttpServletRequest.class);
//            when(request.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(BEARER + " " + token);
//            when(request.getHeader(HttpConstants.SCOPE)).thenReturn(TENANT);
//
//            filter.filter(ctx, request);
//
//            assertFalse(filter.getInvalidCache().asMap().containsKey(token));
//            assertTrue(filter.getValidCache().asMap().containsKey(token));
//            assertEquals(token, filter.getValidCache().getIfPresent(token));
//            assertTrue(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//            assertEquals(BEARER + " " + token, zuulRequestHeaders.get(HttpConstants.AUTHORIZATION));
//
//            verify(filter, times(0)).verifyAndAuthenticate(anyString(), anyString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void invalid_regards_token_is_verified_against_authentication_service_and_cached_as_invalid_if_verification_fails() {
//        try {
//            String token = UUID.randomUUID().toString();
//
//            JWTAuthentication authentication = mock(JWTAuthentication.class);
//            when(authentication.getTenant()).thenReturn(TENANT);
//            when(authentication.getJwt()).thenReturn(token);
//            doNothing().when(authentication).setTenant(any());
//            JwtException expected = new JwtException("Expected");
//            when(jwtService.parseToken(any())).thenThrow(expected);
//
//            RequestContext ctx = mock(RequestContext.class);
//            HashMap<String, String> zuulRequestHeaders = new HashMap<>();
//            when(ctx.getZuulRequestHeaders()).thenReturn(zuulRequestHeaders);
//
//            HttpServletRequest request = mock(HttpServletRequest.class);
//            when(request.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(BEARER + " " + token);
//            when(request.getHeader(HttpConstants.SCOPE)).thenReturn(TENANT);
//
//            RuntimeException err = new RuntimeException("Expected");
//            doThrow(err)
//                .when(filter)
//                .verifyAndAuthenticate(anyString(), anyString());
//
//            filter.filter(ctx, request);
//
//            assertFalse(filter.getValidCache().asMap().containsKey(token));
//            assertTrue(filter.getInvalidCache().asMap().containsKey(token));
//            assertFalse(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//
//            verify(filter).verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt());
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void invalid_regards_token_is_verified_against_authentication_service_and_cached_as_valid_if_verification_succeeds() {
//        try {
//            String token = UUID.randomUUID().toString();
//
//            JWTAuthentication authentication = mock(JWTAuthentication.class);
//            when(authentication.getTenant()).thenReturn(TENANT);
//            when(authentication.getJwt()).thenReturn(token);
//            doNothing().when(authentication).setTenant(any());
//            JwtException expected = new JwtException("Expected");
//            when(jwtService.parseToken(any())).thenThrow(expected);
//
//            RequestContext ctx = mock(RequestContext.class);
//            HashMap<String, String> zuulRequestHeaders = new HashMap<>();
//            when(ctx.getZuulRequestHeaders()).thenReturn(zuulRequestHeaders);
//
//            HttpServletRequest request = mock(HttpServletRequest.class);
//            when(request.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(BEARER + " " + token);
//            when(request.getHeader(HttpConstants.SCOPE)).thenReturn(TENANT);
//
//            String newToken = UUID.randomUUID().toString();
//            doReturn(newToken)
//                .when(filter)
//                .verifyAndAuthenticate(anyString(), anyString());
//
//            filter.filter(ctx, request);
//
//            assertFalse(filter.getInvalidCache().asMap().containsKey(token));
//            assertTrue(filter.getValidCache().asMap().containsKey(token));
//            assertEquals(newToken, filter.getValidCache().getIfPresent(token));
//            assertTrue(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//            assertEquals(BEARER + " " + newToken, zuulRequestHeaders.get(HttpConstants.AUTHORIZATION));
//
//            verify(filter).verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt());
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void cached_invalid_token_is_not_checked_at_all() {
//        try {
//            String token = UUID.randomUUID().toString();
//            String mappedToken = UUID.randomUUID().toString();
//
//            filter.getValidCache().put(token, mappedToken);
//
//            RequestContext ctx = mock(RequestContext.class);
//
//            HttpServletRequest request = mock(HttpServletRequest.class);
//            when(request.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(BEARER + " " + token);
//            when(request.getHeader(HttpConstants.SCOPE)).thenReturn(TENANT);
//
//            filter.filter(ctx, request);
//
//            verifyNoInteractions(jwtService, runtimeTenantResolver, externalAuthenticationClient);
//            verify(filter, times(0)).verifyAndAuthenticate(anyString(), anyString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void cached_valid_token_is_passed_downstream() {
//        try {
//            String token = UUID.randomUUID().toString();
//            String mappedToken = UUID.randomUUID().toString();
//
//            filter.getValidCache().put(token, mappedToken);
//
//            RequestContext ctx = mock(RequestContext.class);
//            HashMap<String, String> zuulRequestHeaders = new HashMap<>();
//            when(ctx.getZuulRequestHeaders()).thenReturn(zuulRequestHeaders);
//
//            HttpServletRequest request = mock(HttpServletRequest.class);
//            when(request.getHeader(HttpConstants.AUTHORIZATION)).thenReturn(BEARER + " " + token);
//            when(request.getHeader(HttpConstants.SCOPE)).thenReturn(TENANT);
//
//            filter.filter(ctx, request);
//
//            assertTrue(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//            assertEquals(BEARER + " " + mappedToken, zuulRequestHeaders.get(HttpConstants.AUTHORIZATION));
//
//            verifyNoInteractions(jwtService, runtimeTenantResolver, externalAuthenticationClient);
//            verify(filter, times(0)).verifyAndAuthenticate(anyString(), anyString());
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
//
//    @Test
//    public void verify_fail_when_client_fails() {
//        HttpClientErrorException: {
//            HttpClientErrorException expected = new HttpClientErrorException(HttpStatus.BAD_REQUEST);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> filter.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(InternalAuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//
//        HttpServerErrorException: {
//            HttpServerErrorException expected = new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> filter.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(AuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//
//        FeignException: {
//            FeignException expected = mock(FeignException.class);
//            doThrow(expected)
//                .when(externalAuthenticationClient)
//                .verifyAndAuthenticate(anyString());
//            assertThatThrownBy(() -> filter.verifyAndAuthenticate("plop", "plop"))
//                .isExactlyInstanceOf(InternalAuthenticationServiceException.class)
//                .hasCauseReference(expected);
//        }
//    }
//
//    @Test
//    public void verify_fail_when_server_returns_unexpected_status_code() {
//        doReturn(ResponseEntity.noContent().build())
//            .when(externalAuthenticationClient)
//            .verifyAndAuthenticate(anyString());
//        assertThatThrownBy(() -> filter.verifyAndAuthenticate("plop", "plop"))
//            .isExactlyInstanceOf(InsufficientAuthenticationException.class);
//    }
}
