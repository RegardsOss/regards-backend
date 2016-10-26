/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.security.endpoint;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.core.Authentication;

import fr.cnes.regards.framework.security.utils.endpoint.RoleAuthority;

/**
 *
 * Class RootAccessVoterTest
 *
 * Test class for RootAccessVoter
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class RootAccessVoterTest {

    /**
     *
     * Test that the specific role ROOT_ADMIN can access every endpoints
     *
     * @since 1.0-SNAPSHOT
     */
    @Test
    public void testRootAccessVoterVote() {

        final Authentication authenticationMock = Mockito.mock(Authentication.class);

        final Collection<RoleAuthority> authorities = new ArrayList<>();
        authorities.add(new RoleAuthority(RootResourceAccessVoter.ROOT_ADMIN_AUHTORITY));
        Mockito.doReturn(authorities).when(authenticationMock).getAuthorities();

        final RootResourceAccessVoter voter = new RootResourceAccessVoter();
        int result = voter.vote(authenticationMock, this.getClass().getMethods()[0], null);
        Assert.assertEquals(result, AccessDecisionVoter.ACCESS_GRANTED);

        authorities.clear();
        authorities.add(new RoleAuthority("USER"));
        Mockito.doReturn(authorities).when(authenticationMock).getAuthorities();
        result = voter.vote(authenticationMock, this.getClass().getMethods()[0], null);
        Assert.assertEquals(result, AccessDecisionVoter.ACCESS_DENIED);

    }

}
