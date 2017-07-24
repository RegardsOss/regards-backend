#!/usr/bin/env groovy

/*
 * LICENSE_PLACEHOLDER
 */

/**
 * Declaratve Jenkinsfile. The language is Groovy.
 * Contains the definition of a Jenkins Pipeline, is checked into source control
 * and is expected to be the single source of truth.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @see https://jenkins.io/doc/book/pipeline/jenkinsfile/
 */
pipeline {
    agent any
    tools {
        maven 'Maven 3.3.9'
        jdk 'jdk8'
    }

    stages {
        stage('Deploy & Analyze') {
            when {
                anyOf {
                    branch 'master'; branch 'develop'; branch '1.0-RELEASE'
                }
            }
            steps {
                sh 'mvn -U -P delivery clean org.jacoco:jacoco-maven-plugin:0.7.7.201606060606:prepare-agent ' +
                        'deploy sonar:sonar -Dspring.profiles.active=rabbit ' +
                        '-Dsonar.jacoco.reportPath=${WORKSPACE}/jacoco-ut.exec ' +
                        '-Dsonar.jacoco.itReportPath=${WORKSPACE}/jacoco-it.exec ' +
                        '-Dsonar.branch=${env.BRANCH_NAME}'
                // TODO build and push docker image
            }
        }
        stage('Verify') {
            when {
                not {
                    anyOf {
                        branch 'master'; branch 'develop'; branch '1.0-RELEASE'
                    }
                }
            }
            steps {
                sh 'mvn -U -P delivery clean verify sonar:sonar -Dspring.profiles.active=rabbit '
            }
        }
    }
}