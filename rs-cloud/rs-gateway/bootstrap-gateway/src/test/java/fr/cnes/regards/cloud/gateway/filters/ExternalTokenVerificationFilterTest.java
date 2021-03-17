package fr.cnes.regards.cloud.gateway.filters;

public class ExternalTokenVerificationFilterTest {
//    public static final String TENANT = "DEFAULT";
//    @Mock
//    private JWTService jwtService;
//
//    @Mock
//    private IExternalAuthenticationResolver externalAuthenticationResolver;
//
//    private ExternalTokenVerificationFilter filter;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        filter = new ExternalTokenVerificationFilter(jwtService, externalAuthenticationResolver);
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
//            verifyNoInteractions(externalAuthenticationResolver);
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
//            when(externalAuthenticationResolver.verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt()))
//                .thenThrow(err);
//
//            filter.filter(ctx, request);
//
//            assertFalse(filter.getValidCache().asMap().containsKey(token));
//            assertTrue(filter.getInvalidCache().asMap().containsKey(token));
//            assertFalse(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//
//            verify(externalAuthenticationResolver).verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt());
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
//            when(externalAuthenticationResolver.verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt()))
//                .thenReturn(newToken);
//
//            filter.filter(ctx, request);
//
//            assertFalse(filter.getInvalidCache().asMap().containsKey(token));
//            assertTrue(filter.getValidCache().asMap().containsKey(token));
//            assertEquals(newToken, filter.getValidCache().getIfPresent(token));
//            assertTrue(zuulRequestHeaders.containsKey(HttpConstants.AUTHORIZATION));
//            assertEquals(BEARER + " " + newToken, zuulRequestHeaders.get(HttpConstants.AUTHORIZATION));
//
//            verify(externalAuthenticationResolver).verifyAndAuthenticate(authentication.getTenant(), authentication.getJwt());
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
//            verifyNoInteractions(jwtService, externalAuthenticationResolver);
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
//            verifyNoInteractions(jwtService, externalAuthenticationResolver);
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail();
//        }
//    }
}
