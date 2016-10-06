/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.administration.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.bind.annotation.RequestMethod;

import fr.cnes.regards.microservices.core.security.endpoint.MethodAuthorizationService;
import fr.cnes.regards.microservices.core.test.AbstractRegardsIntegrationTest;
import fr.cnes.regards.microservices.core.test.report.annotation.Purpose;
import fr.cnes.regards.microservices.core.test.report.annotation.Requirement;
import fr.cnes.regards.modules.emails.service.IEmailService;
import fr.cnes.regards.security.utils.jwt.JWTService;

/**
 * Integration tests for the email module
 *
 * @author xbrochar
 *
 */
public class EmailControllerIT extends AbstractRegardsIntegrationTest {

    @Autowired
    private JWTService jwtService_;

    @Autowired
    private MethodAuthorizationService authService_;

    private String jwt_;

    @Autowired
    private IEmailService emailService_;

    // @Rule
    // public ExpectedException thrown_ = ExpectedException.none();

    @Value("${root.admin.login:admin}")
    private String rootAdminLogin_;

    @Value("${root.admin.password:admin}")
    private String rootAdminPassword_;

    @Before
    public void init() {
        setLogger(LoggerFactory.getLogger(EmailControllerIT.class));
        jwt_ = jwtService_.generateToken("PROJECT", "email", "SVG", "USER");
        authService_.setAuthorities("/emails", RequestMethod.GET, "USER");
        authService_.setAuthorities("/emails", RequestMethod.POST, "USER");
        authService_.setAuthorities("/emails/{mail_id}", RequestMethod.GET, "USER");
        authService_.setAuthorities("/emails/{mail_id}", RequestMethod.PUT, "USER");
        authService_.setAuthorities("/emails/{mail_id}", RequestMethod.DELETE, "USER");
    }

    @Test
    @Requirement("REGARDS_DSL_ADM_ADM_440")
    @Requirement("REGARDS_DSL_ADM_ADM_450")
    @Purpose("Check that the system allows to retrieve the list of sent emails.")
    public void retrieveEmails() {
        List<ResultMatcher> expectations = new ArrayList<>(1);
        expectations.add(status().isOk());
        performGet("/emails", jwt_, expectations, "Unable to retrieve the emails list");
    }

}
