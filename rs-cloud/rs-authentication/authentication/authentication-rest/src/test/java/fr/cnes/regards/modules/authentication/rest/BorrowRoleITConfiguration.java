/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.authentication.rest;

import java.util.List;

import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.collect.Lists;

import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.swagger.autoconfigure.SwaggerAutoConfiguration;
import fr.cnes.regards.modules.accessrights.client.IRolesClient;
import fr.cnes.regards.modules.accessrights.domain.projects.Role;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@ComponentScan(basePackages = { "fr.cnes.regards.framework.authentication.role" })
@EnableAutoConfiguration(exclude = { SwaggerAutoConfiguration.class })
@Configuration
public class BorrowRoleITConfiguration {

    @Bean
    public IRolesClient rolesClient() {
        Role rolePublic = new Role(DefaultRole.PUBLIC.toString(), null);
        rolePublic.setNative(true);
        Role roleRegisteredUser = new Role(DefaultRole.REGISTERED_USER.toString(), rolePublic);
        roleRegisteredUser.setNative(true);
        Role roleAdmin = new Role(DefaultRole.ADMIN.toString(), roleRegisteredUser);
        roleAdmin.setNative(true);
        Role roleProjectAdmin = new Role(DefaultRole.PROJECT_ADMIN.toString(), null);
        roleProjectAdmin.setNative(true);
        IRolesClient roleClient = Mockito.mock(IRolesClient.class);
        List<Role> borrowables = Lists.newArrayList(roleAdmin, roleRegisteredUser, rolePublic);
        Mockito.when(roleClient.getBorrowableRoles()).thenReturn(new ResponseEntity<List<Resource<Role>>>(
                HateoasUtils.wrapList(borrowables), HttpStatus.OK));
        return roleClient;
    }

}
