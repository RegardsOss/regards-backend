#!/usr/bin/env groovy

/*
 * LICENSE_PLACEHOLDER
 */

/**
 * Declaratve Jenkinsfile. The language is Groovy.
 * Contains the definition of a Jenkins Pipeline, is checked into source control
 * and is expected to be the reference.
 *
 * @author Sylvain VISSIERE-GUERINET
 * @see https://jenkins.io/doc/book/pipeline/jenkinsfile/
 */
pipeline {
    agent { label 'unix-integration' }

    stages {
        stage('Deploy artifacts') {
            when {
                expression { BRANCH_NAME ==~ /(master|develop.*|release.*)/ }
            }
            steps {
                sh 'mvn -U clean org.jacoco:jacoco-maven-plugin:0.7.7.201606060606:prepare-agent deploy sonar:sonar -Dsonar.branch=${BRANCH_NAME}'
            }
        }
    }
}