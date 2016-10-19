/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;

/**
 *
 * Class DaoUtils
 *
 * Tools class for DAO
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class DaoUtils {

    /**
     * Package to scan for DAO Entities and Repositories
     */
    public static final String PACKAGES_TO_SCAN = "fr.cnes.regards";

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DaoUtils.class);

    /**
     *
     * Constructor
     *
     * @since 1.0-SNAPSHOT
     */
    private DaoUtils() {
    }

    /**
     *
     * This method check that the classPatch is valid. That the scan packages for instance database and the projects
     * database are not in conflict.
     *
     * @throws MultiDataBasesException
     *
     * @since 1.0-SNAPSHOT
     */
    public static void checkClassPath(final String packageToScan) throws MultiDataBasesException {

        LOG.info("Checking classpath for conflicts between instance and projects databases ...");

        final List<Class<?>> instanceClasses = DaoUtils.scanForJpaPackages(packageToScan, InstanceEntity.class, null);
        final List<Class<?>> projectsClasses = DaoUtils.scanForJpaPackages(packageToScan, Entity.class,
                                                                           InstanceEntity.class);
        final List<String> instancePackages = new ArrayList<>();
        instanceClasses.forEach(instanceClass -> instancePackages.add(instanceClass.getPackage().getName()));
        final List<String> projectPackages = new ArrayList<>();
        projectsClasses.forEach(projectClass -> projectPackages.add(projectClass.getPackage().getName()));
        final boolean errorFound = false;
        instancePackages.forEach(instancePackage -> {
            for (final String pack : projectPackages) {
                if (pack.contains(instancePackage) || instancePackage.contains(pack)) {
                    LOG.error(String.format(
                                            "Invalid classpath. Package %s is used for instance DAO Entities and multitenant DAO Entities",
                                            instancePackage));
                }
            }
        });

        if (errorFound) {
            throw new MultiDataBasesException("Invalid classpath for JPA multitenant and JPA instance databases.");
        }

        LOG.info("Classpath is valid !");

    }

    /**
     *
     * Scan classpath into given package with given filters and return matching classes
     *
     * @param pPackageToScan
     *            Package to scan
     * @param pIncludeAnnotation
     *            Include filter
     * @param pExcludeAnnotation
     *            Exclude filter
     * @return matching classes
     * @since 1.0-SNAPSHOT
     */
    public static List<Class<?>> scanForJpaPackages(final String pPackageToScan,
            final Class<? extends Annotation> pIncludeAnnotation,
            final Class<? extends Annotation> pExcludeAnnotation) {
        final List<Class<?>> packages = new ArrayList<>();
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        if (pExcludeAnnotation != null) {
            scanner.addExcludeFilter(new AnnotationTypeFilter(pExcludeAnnotation));
        }
        if (pIncludeAnnotation != null) {
            scanner.addIncludeFilter(new AnnotationTypeFilter(pIncludeAnnotation));
        }
        for (final BeanDefinition def : scanner.findCandidateComponents(pPackageToScan)) {
            try {
                packages.add(Class.forName(def.getBeanClassName()));
            } catch (final ClassNotFoundException e) {
                LOG.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOG.error(e.getMessage(), e);
            }
        }
        return packages;
    }

}
