def call() {
    pipeline {
        agent any
        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }
            stage('Build') {
                steps {
                    sh 'mvn clean install'
                }
            }
            stage('Test') {
                steps {
                    sh 'mvn test'
                }
            }
            stage('Build Docker Image') {
                steps {
                    script {
                        def image = docker.build("pramila188/testhello:${env.BUILD_ID}")
                        image.push()
                    }
                }
            }
            stage('Deploy') {
                steps {
                    sh 'kubectl apply -f deployment.yaml'
                }
            }
        }
        post {
            always {
                cleanWs()
            }
        }
    }
}
