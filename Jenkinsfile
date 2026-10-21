pipeline {
    agent any

    tools {
        jdk 'jdk21'
    }

    stages {
        stage('Build') {
            steps {
                sh './gradlew clean compileJava compileTestJava --no-daemon'
            }
        }
        stage('Test') {
            steps {
                sh './gradlew test --no-daemon'
            }
            post {
                always {
                    junit 'build/test-results/test/*.xml'
                }
            }
        }
        stage('Report') {
            steps {
                sh './gradlew build -x test --no-daemon'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
