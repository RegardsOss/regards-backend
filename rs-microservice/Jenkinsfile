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
        stage('Clean & Prepare') {
            sh 'mvn -U -P delivery clean org.jacoco:jacoco-maven-plugin:0.7.7.201606060606:prepare-agent'
        }
        stage('Compile') {
            sh 'mvn -U -P delivery compile'
        }
        stage('Unit Testing') {
            sh 'mvn -U -P delivery test -Dsonar.jacoco.reportPath=${WORKSPACE}/jacoco-ut.exec'
        }
        stage('Integration Testing') {
            sh 'mvn -U -P delivery verify -Dspring.profiles.active=rabbit -Dsonar.jacoco.itReportPath=${WORKSPACE}/jacoco-it.exec'
        }
        stage('Deploy') {
            sh 'mvn -U -P delivery deploy'
        }
        stage('sonar') {
            sh 'mvn -U -P delivery sonar:sonar '
        }
    }
//    -U
//    -P delivery
//    clean
//    org.jacoco:jacoco-maven-plugin:0.7.7.201606060606:prepare-agent
//    deploy
//    sonar:sonar
//    -Dspring.profiles.active=rabbit
//    -Dsonar.jacoco.reportPath=${WORKSPACE}/jacoco-ut.exec
//    -Dsonar.jacoco.itReportPath=${WORKSPACE}/jacoco-it.exec

}