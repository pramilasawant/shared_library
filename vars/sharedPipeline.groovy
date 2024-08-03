def call() {
    pipeline {
        agent any
        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhunpwd')
            SLACK_CREDENTIALS = credentials('jen-slack-pwd')
        }
        parameters {
               string(name: 'JAVA_REPO', defaultValue: 'https://github.com/pramilasawant/testhello.git', description: 'Java Application Repository')
               string(name: 'PYTHON_REPO', defaultValue: 'https://github.com/pramilasawant/phython-application.git', description: 'Python Application Repository')
               string(name: 'DOCKERHUB_USERNAME', defaultValue: 'pramila188', description: 'DockerHub Username')
               string(name: 'JAVA_IMAGE_NAME', defaultValue: 'testhello', description: 'Java Docker Image Name')
               string(name: 'PYTHON_IMAGE_NAME', defaultValue: 'python-app', description: 'Python Docker Image Name')
               string(name: 'JAVA_NAMESPACE', defaultValue: 'test', description: 'Kubernetes Namespace for Java Application')
               string(name: 'PYTHON_NAMESPACE', defaultValue: 'python', description: 'Kubernetes Namespace for Python Application')
            
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
            stage('Get Approval') {
                steps {
                    script {
                        input message: 'Do you approve this deployment?', ok: 'Yes, deploy'
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
                slackSend(color: '#00FF00', message: "SUCCESS: ${env.JOB_NAME} #${env.BUILD_NUMBER}")
            }
            failure {
                slackSend(color: '#FF0000', message: "FAILURE: ${env.JOB_NAME} #${env.BUILD_NUMBER}")
            }
        }
    }
}
