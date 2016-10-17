// package fr.cnes.regards.microservices.administration;
//
// import java.util.HashMap;
// import java.util.Map;
//
// import javax.sql.DataSource;
//
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Component;
//
// import fr.cnes.regards.framework.jpa.multitenant.autoconfigure.IMultitenantConnectionsReader;
// import fr.cnes.regards.framework.jpa.multitenant.properties.MultitenantDaoProperties;
// import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
// import fr.cnes.regards.modules.core.exception.EntityNotFoundException;
// import fr.cnes.regards.modules.project.domain.Project;
// import fr.cnes.regards.modules.project.domain.ProjectConnection;
// import fr.cnes.regards.modules.project.service.ProjectService;
//
// @Component
// public class ProjectsDAOConfiguration implements IMultitenantConnectionsReader {
//
// private static final Logger LOG = LoggerFactory.getLogger(ProjectsDAOConfiguration.class);
//
// @Value("${spring.application.name")
// private String microserviceName;
//
// @Autowired
// private MultitenantDaoProperties daoProperties;
//
// @Autowired
// private ProjectService projectService;
//
// @Override
// public Map<String, DataSource> getDataSources() {
// final Iterable<Project> projects = projectService.retrieveProjectList();
//
// final Map<String, DataSource> datasources = new HashMap<>();
//
// for (final Project project : projects) {
// if (daoProperties.getEmbedded()) {
// datasources.put(project.getName(), DataSourceHelper
// .createEmbeddedDataSource(project.getName(), daoProperties.getEmbeddedPath()));
// } else {
// ProjectConnection projectConnection;
// try {
// projectConnection = projectService.retreiveProjectConnection(project.getName(), microserviceName);
// datasources.put(project.getName(), DataSourceHelper
// .createDataSource(projectConnection.getUrl(), projectConnection.getDriverClassName(),
// projectConnection.getUserName(), projectConnection.getPassword()));
// } catch (final EntityNotFoundException e) {
// LOG.error(e.getMessage(), e);
// LOG.error(String.format("No database connection found for project %s", project.getName()));
// }
// }
//
// }
//
// return datasources;
//
// }
//
// }
