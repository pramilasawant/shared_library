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
                    sh 'python setup.py install'
                }
            }
            stage('Test') {
                steps {
                    sh 'pytest'
                }
            }
            stage('Build Docker Image') {
                steps {
                    script {
                        def image = docker.build("pramila188/python-app:${env.BUILD_ID}")
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
