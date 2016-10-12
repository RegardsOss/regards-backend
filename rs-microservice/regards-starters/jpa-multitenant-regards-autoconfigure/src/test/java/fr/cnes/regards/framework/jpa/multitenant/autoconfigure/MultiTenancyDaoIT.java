/// *
// * LICENSE_PLACEHOLDER
// */
// package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;
//
// import org.junit.Assert;
// import org.junit.Test;
// import org.junit.runner.RunWith;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
// import org.springframework.http.HttpHeaders;
// import org.springframework.test.annotation.DirtiesContext;
// import org.springframework.test.context.ContextConfiguration;
// import org.springframework.test.context.junit4.SpringRunner;
// import org.springframework.test.context.web.WebAppConfiguration;
// import org.springframework.test.web.servlet.MockMvc;
// import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
// import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
// import org.springframework.web.bind.annotation.RequestMethod;
//
// import fr.cnes.regards.framework.starter.security.endpoint.MethodAuthorizationService;
// import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
// import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
// import fr.cnes.regards.security.utils.jwt.JWTService;
//
/// **
// *
// * Class MultiTenancyDaoIT
// *
// * Integration test for DAO From Rest request to database to check the multitenancy tenant resolver from JWT Token
// *
// * @author CS
// * @since 1.0-SNAPSHOT
// */
// @RunWith(SpringRunner.class)
// @ContextConfiguration(classes = { MultiTenancyDaoITConfiguration.class })
// @WebAppConfiguration
// @AutoConfigureMockMvc
// @DirtiesContext
// public class MultiTenancyDaoIT {
//
// /**
// * Bearer label in HTTP request header
// */
// private static final String BEARER = "Bearer ";
//
// /**
// * Role admin for this test
// */
// private static final String TEST_ROLE = "ADMIN";
//
// /**
// * Mock Mvc to simulate rest requests.
// */
// @Autowired
// private MockMvc mockMvc;
//
// /**
// * Security JWT management service
// */
// @Autowired
// private JWTService jwtService;
//
// /**
// * Security authorization management service
// */
// @Autowired
// private MethodAuthorizationService authService;
//
// /**
// *
// * Integration test to check that the tenant is passing through the authentication token from rest request to
// * database
// *
// * @since 1.0-SNAPSHOT
// */
// @Requirement("REGARDS_DSL_SYS_ARC_050")
// @Purpose("Integration test .Check tenant is passing through the authentication token from rest request to database")
// @Test
// public void testMvc() {
//
// final String tokenTest1 = jwtService.generateToken("test1", "name", "lastname", TEST_ROLE);
// final String tokenTest2 = jwtService.generateToken("invalid", "name2", "lastname2", TEST_ROLE);
// final String getUsersRessources = "/test/dao/users";
// final String getProjectsRessources = "/test/dao/projects";
//
// authService.setAuthorities(getUsersRessources, RequestMethod.GET, TEST_ROLE);
// authService.setAuthorities(getProjectsRessources, RequestMethod.POST, TEST_ROLE);
//
// try {
//
// // Run with valid tenant with multi tenant DAO
// mockMvc.perform(MockMvcRequestBuilders.get(getUsersRessources).header(HttpHeaders.AUTHORIZATION,
// BEARER + tokenTest1))
// .andExpect(MockMvcResultMatchers.status().isOk());
//
// // Run with invalid tenant with multi tenant DAO
// mockMvc.perform(MockMvcRequestBuilders.get(getUsersRessources).header(HttpHeaders.AUTHORIZATION,
// BEARER + tokenTest2))
// .andExpect(MockMvcResultMatchers.status().isInternalServerError());
//
// // Run with valid tenant with an instance DAO
// mockMvc.perform(MockMvcRequestBuilders.get(getProjectsRessources).header(HttpHeaders.AUTHORIZATION,
// BEARER + tokenTest1))
// .andExpect(MockMvcResultMatchers.status().isOk());
//
// } catch (Exception e) {
// Assert.fail(e.getStackTrace().toString());
// }
// }
//
// }
