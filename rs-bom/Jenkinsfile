#!/usr/bin/env groovy

/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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