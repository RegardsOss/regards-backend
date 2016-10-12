/// *
// * LICENSE_PLACEHOLDER
// */
// package fr.cnes.regards.framework.jpa.multitenant.autoconfigure;
//
// import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
// import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
// import org.springframework.boot.context.properties.EnableConfigurationProperties;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.FilterType;
// import org.springframework.context.annotation.PropertySource;
//
// import fr.cnes.regards.framework.starter.jpa.configuration.MicroserviceConfiguration;
// import fr.cnes.regards.framework.starter.jpa.service.DaoUserTest;
// import fr.cnes.regards.framework.starter.jpa.utils.CurrentTenantIdentifierResolverMock;
//
/// **
// *
// * Class MultiTenancyDaoITConfiguration
// *
// * Configuration file for DAO integration tests
// *
// * @author CS
// * @since 1.0-SNAPSHOTS
// */
// @ComponentScan(basePackages = { "fr.cnes.regards.microservices.core.dao",
/// "fr.cnes.regards.microservices.core.security",
// "fr.cnes.regards.security.utils" }, excludeFilters = {
// @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
// CurrentTenantIdentifierResolverMock.class, DaoUserTest.class }) })
// @EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class })
// @EnableConfigurationProperties(MicroserviceConfiguration.class)
// @PropertySource("classpath:dao.properties")
// @PropertySource("classpath:jwt.properties")
// public class MultiTenancyDaoITConfiguration {
//
// }
