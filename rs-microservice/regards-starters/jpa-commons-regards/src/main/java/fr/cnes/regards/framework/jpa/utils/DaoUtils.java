/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.framework.jpa.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import fr.cnes.regards.framework.jpa.annotation.InstanceEntity;
import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;

/**
 * Class DaoUtils
 *
 * Tools class for DAO
 * @author Sébastien Binda
 * @author oroussel
 */
public final class DaoUtils {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DaoUtils.class);

    /**
     * Root package
     */
    public static final String ROOT_PACKAGE = "fr.cnes.regards";

    /**
     * Framework root package
     */
    public static final String FRAMEWORK_PACKAGE = "fr.cnes.regards.framework.modules";

    /**
     * Test Framework root package
     */
    public static final String TEST_PACKAGE = "fr.cnes.regards.framework.test";

    /**
     * Modules root package
     */
    public static final String MODULES_PACKAGE = "fr.cnes.regards.modules";

    private DaoUtils() {
    }

    /**
     * This method check that the classPatch is valid. That the scan packages for instance database and the projects
     * database are not in conflict.
     * @param pPackageToScan package name to scan for JPA entities and repositories
     */
    public static void checkClassPath(final String pPackageToScan) throws MultiDataBasesException {
        LOGGER.info("Checking classpath for conflicts between instance and projects databases ...");

        final Set<String> packagesToScan = findPackagesForJpa(pPackageToScan);
        final List<Class<?>> instanceClasses = DaoUtils.scanPackagesForJpa(InstanceEntity.class, null, packagesToScan);
        final List<Class<?>> projectsClasses = DaoUtils
                .scanPackagesForJpa(Entity.class, InstanceEntity.class, packagesToScan);
        final List<String> instancePackages = new ArrayList<>();
        instanceClasses.forEach(instanceClass -> instancePackages.add(instanceClass.getPackage().getName()));
        final List<String> projectPackages = new ArrayList<>();
        projectsClasses.forEach(projectClass -> projectPackages.add(projectClass.getPackage().getName()));
        for (final String instancePackage : instancePackages) {
            for (final String pack : projectPackages) {
                if (pack.contains(instancePackage) || instancePackage.contains(pack)) {
                    LOGGER.error(String.format(
                            "Invalid classpath. Package %s is used for instance DAO Entities and multitenant DAO Entities",
                            instancePackage));
                    throw new MultiDataBasesException(
                            "Invalid classpath for JPA multitenant and JPA instance databases.");
                }
            }
        }

        LOGGER.info("Classpath is valid !");

    }

    /**
     * Find the packages for the JPA. Find the class who used Jpa and extracts their package name.
     * Find all the class annotated with {@link org.springframework.data.repository.Repository}
     * Find all the class who extends {@link CrudRepository}
     * Find all the class who extends {@link JpaRepository}
     * @param pRootPackage the base package
     * @return the {@link Set} of package
     */
    @SuppressWarnings("rawtypes")
    public static Set<String> findPackagesForJpa(String pRootPackage) {
        Set<String> packagesToScan = new HashSet<>();
        final Reflections reflections = new Reflections(pRootPackage);

        // Add the packages that contains a class annotated with org.springframework.stereotype.Repository
        final Set<Class<?>> annotatedRepository = reflections.getTypesAnnotatedWith(Repository.class);
        for (Class<?> aClass : annotatedRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        // Add the packages that contains a class annotated with org.springframework.data.jpa.repository.JpaRepository
        final Set<Class<? extends JpaRepository>> subTypeJpaRepository = reflections.getSubTypesOf(JpaRepository.class);
        for (Class<?> aClass : subTypeJpaRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        // Add the packages that contains a class annotated with org.springframework.data.repository.CrudRepository
        final Set<Class<? extends CrudRepository>> subTypeRepository = reflections.getSubTypesOf(CrudRepository.class);
        for (Class<?> aClass : subTypeRepository) {
            packagesToScan.add(getPackageToScan(aClass.getCanonicalName()));
        }

        return packagesToScan;
    }

    /**
     * package name has format : fr.cnes.regards.modules.(name)....
     * or fr.cnes.regards.framework.modules.(name)...
     *
     * We must only take fr.cnes.regards[.framework].modules.(name)
     */
    public static String getPackageToScan(String pPackageName) {
        if (pPackageName.startsWith(DaoUtils.FRAMEWORK_PACKAGE)) {
            String packageEnd = pPackageName.substring(DaoUtils.FRAMEWORK_PACKAGE.length() + 1);
            return DaoUtils.FRAMEWORK_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.'));
        } else if (pPackageName.startsWith(DaoUtils.MODULES_PACKAGE)) {
            String packageEnd = pPackageName.substring(DaoUtils.MODULES_PACKAGE.length() + 1);
            return DaoUtils.MODULES_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.'));
        } else if (pPackageName.startsWith(DaoUtils.TEST_PACKAGE)) {
            String packageEnd = pPackageName.substring(DaoUtils.TEST_PACKAGE.length() + 1);
            return DaoUtils.TEST_PACKAGE + "." + packageEnd.substring(0, packageEnd.indexOf('.'));
        } else {
            throw new Error(String
                    .format("Package %s is not valid. REGARDS only handle classes on package with prefixes : %s, %s and %s",
                            pPackageName, DaoUtils.FRAMEWORK_PACKAGE, DaoUtils.MODULES_PACKAGE, DaoUtils.TEST_PACKAGE)); // NOSONAR
        }
    }

    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, String... pPackages) {
        return Arrays.stream(pPackages)
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    /**
     * Method called by JpaAutoConfiguration entity manager factories to find all elligible Jpa classes
     */
    public static List<Class<?>> scanPackagesForJpa(Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation, Collection<String> pPackages) {
        return pPackages.stream()
                .flatMap(pPackage -> scanPackageForJpa(pPackage, pIncludeAnnotation, pExcludeAnnotation).stream())
                .collect(Collectors.toList());
    }

    /**
     * Scan classpath into given package with given filters and return matching classes
     */
    public static Set<Class<?>> scanPackageForJpa(String pPackageToScan, Class<? extends Annotation> pIncludeAnnotation,
            Class<? extends Annotation> pExcludeAnnotation) {
        return scanPackageForJpa(pPackageToScan, pIncludeAnnotation, pExcludeAnnotation, null);
    }

    private static Set<Class<?>> scanPackageForJpa(String pPackageToScan,
            Class<? extends Annotation> pIncludeAnnotation, Class<? extends Annotation> pExcludeAnnotation,
            Set<Class<?>> pClasses) {
        final Set<Class<?>> classes = (pClasses == null) ? new HashSet<>() : pClasses;
        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false);
        if (pExcludeAnnotation != null) {
            LOGGER.debug("Excluding JPA entities with {} annotation", pExcludeAnnotation.getName());
            scanner.addExcludeFilter(new AnnotationTypeFilter(pExcludeAnnotation));
        }
        if (pIncludeAnnotation != null) {
            LOGGER.debug("Including JPA entities with {} annotation", pIncludeAnnotation.getName());
            scanner.addIncludeFilter(new AnnotationTypeFilter(pIncludeAnnotation));
        }
        // Find all components ie classes annotated with Entity by example
        for (final BeanDefinition def : scanner.findCandidateComponents(pPackageToScan)) {
            try {
                LOGGER.debug("Package {} selected for scanning", def.getBeanClassName());
                Class<?> clazz = Class.forName(def.getBeanClassName());
                // There is one HUGE particular case...When an Entity has a relation (OneToMany, ...) to an external
                // entity whom Repository (ie. dao) is not into classpath (ie. PluginConfiguration for Model)
                Set<Class<?>> inRelationClasses = new HashSet<>();
                for (Field field : clazz.getDeclaredFields()) {
                    Class<?> inRelationClass = null;
                    if (field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class)) {
                        // Must be a List<...> or Set<...> or Bag<...>, not too musch complicated
                        if (field.getGenericType() instanceof ParameterizedType) {
                            inRelationClass = (Class<?>) ((ParameterizedType) field.getGenericType())
                                    .getActualTypeArguments()[0];
                        }
                    } else if (field.isAnnotationPresent(ManyToOne.class) || field
                            .isAnnotationPresent(OneToOne.class)) {
                        inRelationClass = field.getType();
                    }
                    // Adding found class if not already present into classes (to avoid infinite recursion)
                    if ((inRelationClass != null) && !classes.contains(inRelationClass)) {
                        classes.add(inRelationClass);
                        inRelationClasses.add(inRelationClass);
                    }
                }
                // Manage relations into relations (ie. PluginConfiguration -> PluginParameter)
                if (!inRelationClasses.isEmpty()) {
                    for (Class<?> inRelationClass : inRelationClasses) {
                        scanPackageForJpa(getPackageToScan(inRelationClass.getCanonicalName()), pIncludeAnnotation,
                                          pExcludeAnnotation, classes);
                    }
                }
                classes.add(clazz);
            } catch (final ClassNotFoundException e) {
                LOGGER.error("Error adding entity " + def.getBeanClassName() + " for hibernate database update");
                LOGGER.error(e.getMessage(), e);
            }
        }
        return classes;
    }

}
