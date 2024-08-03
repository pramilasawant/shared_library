def call() {
     pipeline {
       agent any
    environment {
        DOCKERHUB_CREDENTIALS = credentials('dockerhunpwd')
        SLACK_CREDENTIALS = credentials('jen-slack-pwd')
    }
    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'main', description: 'Git branch to build')
    }
    stages {
        stage('Clone Repositories') {
            parallel {
                stage('Clone Java Application') {
                    steps {
                        git branch: "${params.GIT_BRANCH}", url: 'https://github.com/pramilasawant/springboot1-application.git'
                    }
                }
                stage('Clone Python Application') {
                    steps {
                        git branch: "${params.GIT_BRANCH}", url: 'https://github.com/pramilasawant/phython-application.git'
                    }
                }
            }
        }
        stage('Build Docker Images') {
            parallel {
                stage('Build Java Image') {
                    steps {
                        script {
                            dockerImage = docker.build("pramila188/testhello")
                        }
                    }
                }
                stage('Build Python Image') {
                    steps {
                        script {
                            dockerImage = docker.build("pramila188/python-app")
                        }
                    }
                }
            }
        }
        stage('Push Docker Images') {
            parallel {
                stage('Push Java Image') {
                    steps {
                        script {
                            docker.withRegistry('', 'dockerhunpwd') {
                                dockerImage.push("${env.BUILD_NUMBER}")
                                dockerImage.push("latest")
                            }
                        }
                    }
                }
                stage('Push Python Image') {
                    steps {
                        script {
                            docker.withRegistry('', 'dockerhunpwd') {
                                dockerImage.push("${env.BUILD_NUMBER}")
                                dockerImage.push("latest")
                            }
                        }
                    }
                }
            }
        }
        stage('Deploy to Kubernetes with helm') {
            parallel {
                stage('Deploy Java Application') {
                    steps {
                        script {
                            sh "helm upgrade --install java-app ./helm/java --set image.repository=pramila188/testhello --set image.tag=${env.BUILD_NUMBER} --namespace test"
                        }
                    }
                }
                stage('Deploy Python Application') {
                    steps {
                        script {
                            sh "helm upgrade --install python-app ./helm/python --set image.repository=pramila188/python-app --set image.tag=${env.BUILD_NUMBER} --namespace python"
                        }
                    }
                }
            }
        }
    }
    post {
        success {
            slackSend (color: '#00FF00', message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}")
        }
        failure {
            slackSend (color: '#FF0000', message: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}")
        }
    }
}
