def call() {
    pipeline {
        agent any

        environment {
            DOCKERHUB_CREDENTIALS = credentials('dockerhubpwd')
            SLACK_CREDENTIALS = credentials('b3ee302b-e782-4d8e-ba83-7fa591d43205')
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
                    stage('Clone Java Repo') {
                        steps {
                            git url: params.JAVA_REPO, branch: 'main'
                        }
                    }
                    stage('Clone Python Repo') {
                        steps {
                            dir('python-app') {
                                git url: params.PYTHON_REPO, branch: 'main'
                            }
                        }
                    }
                }
            }

            stage('Build and Push Docker Images') {
                parallel {
                    stage('Build and Push Java Image') {
                        steps {
                            dir('testhello') { // Ensure this directory contains the pom.xml
                                sh 'mvn clean install'
                                script {
                                    def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME}:${currentBuild.number}")
                                    docker.withRegistry('', 'dockerhubpwd') {
                                        image.push()
                                    }
                                }
                            }
                        }
                    }
                    stage('Build and Push Python Image') {
                        steps {
                            dir('python-app') {
                                script {
                                    def image = docker.build("${params.DOCKERHUB_USERNAME}/${params.PYTHON_IMAGE_NAME}:${currentBuild.number}")
                                    docker.withRegistry('', 'dockerhubpwd') {
                                        image.push()
                                    }
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
                    stage('Deploy Java Application with Helm') {
                        steps {
                            script {
                               sh "helm upgrade --install testhello ./k8s-deployments/java-app --set image.repository=${params.DOCKERHUB_USERNAME}/${params.JAVA_IMAGE_NAME} --set image.tag=${currentBuild.number} --namespace ${params.JAVA_NAMESPACE}"
                            }
                        }
                    }
                    stage('Deploy Python Application with Helm') {
                        steps {
                            script {
                                sh "helm upgrade --install python-app ./k8s-deployments/python-app --set image.repository=${params.DOCKERHUB_USERNAME}/${params.PYTHON_IMAGE_NAME} --set image.tag=${currentBuild.number} --namespace ${params.PYTHON_NAMESPACE}"
                            }
                        }
                    }
                }
            }
        }

        post {
            always {
                script {
                    def slackBaseUrl = 'https://slack.com/api/'
                    def slackChannel = '#builds'
                    def slackColor = currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger'
                    def slackMessage = "Build ${currentBuild.fullDisplayName} finished with status: ${currentBuild.currentResult}"

                    echo "Sending Slack notification to ${slackChannel} with message: ${slackMessage}"

                    slackSend (
                        baseUrl: 'https://yourteam.slack.com/api/',
                        teamDomain: 'StarAppleInfotech',
                        channel: '#builds',
                        color: slackColor,
                        botUser: true,
                        tokenCredentialId: 'b3ee302b-e782-4d8e-ba83-7fa591d43205',
                        notifyCommitters: false,
                        message: "Build Final_project #${env.BUILD_NUMBER} finished with status: ${currentBuild.currentResult}"
                    )
                   
                }
            }
        }
    }
}
