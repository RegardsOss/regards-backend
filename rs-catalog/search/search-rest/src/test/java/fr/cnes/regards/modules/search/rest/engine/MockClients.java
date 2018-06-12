package fr.cnes.regards.modules.search.rest.engine;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.hateoas.Resource;
import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.modules.models.client.IModelAttrAssocClient;
import fr.cnes.regards.modules.models.service.ModelService;
import fr.cnes.regards.modules.project.client.rest.IProjectsClient;
import fr.cnes.regards.modules.project.domain.Project;

@Configuration
public class MockClients {

    @Bean
    IProjectsClient projectsClient() {
        IProjectsClient client = Mockito.mock(IProjectsClient.class);
        Project project = new Project(1L, "Solar system project", "http://plop/icon.png", true, "SolarSystem");
        project.setHost("http://regards/solarsystem");
        ResponseEntity<Resource<Project>> response = ResponseEntity.ok(new Resource<>(project));
        Mockito.when(client.retrieveProject(Mockito.anyString())).thenReturn(response);
        return client;
    }

    @Bean
    IModelAttrAssocClient modelAttrAssocClient(ModelService modelService) {
        IModelAttrAssocClient client = Mockito.mock(IModelAttrAssocClient.class);
        Mockito.when(client.getModelAttrAssocsFor(Mockito.any())).thenAnswer(invocation -> {
            EntityType type = invocation.getArgumentAt(0, EntityType.class);
            return ResponseEntity.ok(modelService.getModelAttrAssocsFor(type));
        });
        return client;
    }

}
