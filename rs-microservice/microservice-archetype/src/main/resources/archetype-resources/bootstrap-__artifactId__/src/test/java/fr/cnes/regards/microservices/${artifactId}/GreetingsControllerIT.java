/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.$

import java.util.ArrayList;

import org.slf4j.LoggerFactory;

import fr.cnes.regards.microservices.administration.AccessesControllerIT;

/**
 * example of integration test using microservice-core-test for greetingsController
 * 
 * @author svissier
 *
 */
public class GreetingsControllerIT extends RegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;
    
    private String jwtAdmin_;
    
    private String jwtUser_;
    
    private String errorMessage;
    
    @Before
    public void init() {
        super(LoggerFactory.getLogger(GreetingsControllerIT.class));
        jwtUser_ = jwtService_.generateToken("PROJECT", "email", "name", "USER");
        jwtUser_ = jwtService_.generateToken("PROJECT", "email", "name", "ADMIN");
        errorMessage = "Cannot reach model attributes";
    }
    
    @Test
    public void getMe() {
        List<ResultMatcher> expectations=new ArrayList<>(1);
        expectations.add(status().isOk());
        // we use jwtAdmin_ because in application.yml endpoint /api/me is only accessible given the role ADMIN
        performGet("/api/me", jwtAdmin_, expectations, errorMessage);
    }
    
    @Test
    public void getGreeting() {
        List<ResultMatcher> expectations=new ArrayList<>(1);
        expectations.add(status().isOk());
        // we use jwtUser_ because in application.yml endpoint /api/greeting is only accessible given the role USER
        performGet("/api/greeting", jwtUser_, expectations, errorMessage);
    }
    
}
