// SeedJobDSL.groovy - Fixed version

def terraformPipeline = pipelineJob('infrastructure-provision') {
    definition {
        cps {
            script('''
pipeline {
    agent any
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    
    parameters {
        string(name: 'GITHUB_CREDENTIAL', defaultValue: 'github_cred', description: 'Github access credentials id')
        string(name: 'GITHUB_REPO_URL', defaultValue: 'https://github.com/ogob31/marketvector.git', description: 'Github repository url')
        string(name: 'GITHUB_BRANCH', defaultValue: 'main', description: 'Github branch for your build')
        choice(name: 'TERRAFORM_ACTION', choices: ['apply', 'destroy'], description: 'Select Terraform action: Apply or Destroy')
    }
    
    environment {
        AWS_REGION = 'us-east-1'
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                checkout([
                    $class: 'GitSCM',
                    branches: [[name: "${params.GITHUB_BRANCH}"]],
                    extensions: [],
                    userRemoteConfigs: [[
                        credentialsId: "${params.GITHUB_CREDENTIAL}",
                        url: "${params.GITHUB_REPO_URL}"
                    ]]
                ])
            }
        }
        
        stage('Terraform Init') {
            steps {
                dir('terraform') {
                    sh 'terraform init'
                }
            }
        }
        
        stage('Terraform Plan') {
            steps {
                dir('terraform') {
                    sh 'terraform plan'
                }
            }
        }
        
        stage('Terraform Apply/Destroy') {
            steps {
                script {
                    dir('terraform') {
                        if (params.TERRAFORM_ACTION == 'destroy') {
                            sh 'terraform destroy -auto-approve'
                        } else {
                            sh 'terraform apply -auto-approve'
                        }
                    }
                }
            }
        }
    }
    
    post {
        always {
            cleanWs()
        }
    }
}
''')
            sandbox(true)
        }
    }
}

def cicdPipeline = pipelineJob('continuous-integration-continuous-deployment') {
    definition {
        cps {
            script('''
pipeline {
    agent any
    options {
        timeout(time: 30, unit: 'MINUTES')
    }
    
    environment {
        AWS_REGION = 'us-east-1'
        ECR_REPOSITORY = 'your-ecr-repo-url'
        ECS_CLUSTER = 'your-ecs-cluster'
        ECS_SERVICE = 'your-ecs-service'
    }
    
    stages {
        stage('Checkout Code') {
            steps {
                git branch: 'main', 
                credentialsId: 'github_cred', 
                url: 'https://github.com/ogob31/marketvector.git'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    docker.build("${ECR_REPOSITORY}:${env.BUILD_ID}")
                }
            }
        }
        
        stage('Push to ECR') {
            steps {
                script {
                    docker.withRegistry("https://${ECR_REPOSITORY}", 'ecr:us-east-1:aws-credentials') {
                        docker.image("${ECR_REPOSITORY}:${env.BUILD_ID}").push()
                    }
                }
            }
        }
        
        stage('Deploy to ECS') {
            steps {
                script {
                    sh """
                    aws ecs update-service \
                        --cluster ${ECS_CLUSTER} \
                        --service ${ECS_SERVICE} \
                        --force-new-deployment \
                        --region ${AWS_REGION}
                    """
                }
            }
        }
    }
}
''')
            sandbox(true)
        }
    }
}

// Return the list of jobs to be created
return [terraformPipeline, cicdPipeline]