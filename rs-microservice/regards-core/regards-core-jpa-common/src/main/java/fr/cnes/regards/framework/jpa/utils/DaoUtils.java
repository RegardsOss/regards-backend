/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.utils;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;

/**
 *
 * Class DaoUtils
 *
 * Tools class for DAO
 *
 * @author Sébastien Binda
 * @since 1.0-SNAPSHOT
 */
public final class DaoUtils {

    /**
     * Root package
     */
    public static final String ROOT_PACKAGE = "fr.cnes.regards";

    /**
     * Framework root package
     */
    public static final String FRAMEWORK_PACKAGE = "fr.cnes.regards.framework";

    /**
     * Modules root package
     */
    public static final String MODULES_PACKAGE = "fr.cnes.regards.modules";

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
     * @param pPackageToScan
     *            package name to scan for JPA entities and repositories
     * @throws MultiDataBasesException
     *
     * @since 1.0-SNAPSHOT
     */
    public static void checkClassPath(final String pPackageToScan) throws MultiDataBasesException {

        LOG.info("Checking classpath for conflicts between instance and projects databases ...");

        final Set<String> packagesToScan = findPackagesForJpa();
        final List<Class<?>> instanceClasses = DaoUtils.scanPackagesForJpa(InstanceEntity.class, null, packagesToScan);
        final List<Class<?>> projectsClasses = DaoUtils.scanPackagesForJpa(Entity.class, InstanceEntity.class,
                                                                           packagesToScan);
        final List<String> instancePackages = new ArrayList<>();
        instanceClasses.forEach(instanceClass -> instancePackages.add(instanceClass.getPackage().getName()));
        final List<String> projectPackages = new ArrayList<>();
        projectsClasses.forEach(projectClass -> projectPackages.add(projectClass.getPackage().getName()));
        for (final String instancePackage : instancePackages) {
            for (final String pack : projectPackages) {
                if (pack.contains(instancePackage) || instancePackage.contains(pack)) {
                    LOG.error(String.format(
                                            "Invalid classpath. Package %s is used for instance DAO Entities and multitenant DAO Entities",
                                            instancePackage));
                    throw new MultiDataBasesException(
                            "Invalid classpath for JPA multitenant and JPA instance databases.");
                }
            }
        }

        LOG.info("Classpath is valid !");

    }

    public static Set<String> findPackagesForJpa() {
        // 2 Add Entity for database mapping from classpath but only for entities from modules that provide repository
        // AND framework entities
        // Why ? If a module depends on a <other_module>-client it contains entities that MUST NOT be taken into account
        // specially if they are from another microservice (rs-admin for example if we are into rs-dam)
        // and framework ones because all framework content is embedded into all micro-services
        Set<String> packagesToScan = new HashSet<>();
        try {
            ClassPath classpath = ClassPath.from(ClassLoader.getSystemClassLoader());
            ImmutableSet<ClassPath.ClassInfo> classInfos = classpath.getAllClasses();
            for (ClassPath.ClassInfo info : classInfos) {
                // Add all framework package if one of its class is into classpath (this always should be the case)
                if (info.getPackageName().startsWith(DaoUtils.FRAMEWORK_PACKAGE)) {
                    packagesToScan.add(DaoUtils.FRAMEWORK_PACKAGE);
                } else
                    if (info.getPackageName().startsWith(DaoUtils.MODULES_PACKAGE)
                            && info.getName().contains("Repository")) {
                        // Restrict to fr.cnes.regards.modules package and search for "Repository" classes
                        // package name has format : fr.cnes.regards.modules.(name)....
                        // We must only take fr.cnes.regards.modules.(name)
                        String packageEnd = info.getPackageName().substring(DaoUtils.MODULES_PACKAGE.length() + 1);
                        packagesToScan
                                .add(DaoUtils.MODULES_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.')));
                    }
            }
        } catch (IOException e) {
            Throwables.propagate(e);
        }
        return packagesToScan;
    }

    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, String... pPackages) {
        return Arrays.stream(pPackages)
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, Collection<String> pPackages) {
        return pPackages.stream()
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    /**
     * Scan classpath into given package with given filters and return matching classes
     * @param pPackageToScan Package to scan
     * @param pIncludeAnnotation Include filter
     * @param pExcludeAnnotation Exclude filter
     * @return matching classes
     */
    public static List<Class<?>> scanPackageForJpa(final String pPackageToScan,
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
