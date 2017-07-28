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
        stage('Init COTS') {
            steps {
                sh 'cd test && docker-compose up -d rs_rabbitmq rs_postgres rs_elasticsearch'
            }
        }
        stage('Deploy & Analyze') {
            when {
                anyOf {
                    branch 'master'; branch 'develop'; branch 'develop_V1.1.0'
                }
            }
            steps {
                // pour l'image docker et récupérer le répo local d'artefact
                // find regards -exec echo `pwd`/{} \; |grep -v pom | grep -v .git | xargs cp -R --target-directory=./testCP/
//                sh 'mvn -U -P delivery clean org.jacoco:jacoco-maven-plugin:0.7.7.201606060606:prepare-agent ' +
//                        'deploy sonar:sonar -Dspring.profiles.active=rabbit ' +
//                        '-Dsonar.jacoco.reportPath=${WORKSPACE}/jacoco-ut.exec ' +
//                        '-Dsonar.jacoco.itReportPath=${WORKSPACE}/jacoco-it.exec ' +
//                        '-Dsonar.branch=${env.BRANCH_NAME}'
                //TODO: add spotify docker plugin into the run.sh file
                sh 'cd test && docker-compose up rs_build_deploy'
            }
        }
        stage('Verify') {
            when {
                not {
                    anyOf {
                        branch 'master'; branch 'develop'; branch 'develop_V1.1.0'
                    }
                }
            }
            steps {
                sh 'cd test && docker-compose up rs_build_verify'
            }
        }
        stage('Clean docker') {
            steps {
                sh 'cd test && docker-compose stop'
            }
        }
    }
}