def summary = manager.createSummary("folder.gif")
summary.appendText("Additional atrifacts:", false)

pipeline {
    options {
        disableConcurrentBuilds()
    }

    agent {
        node {
            label 'cnam'
        }
    }

    parameters {
        choice(name: 'K8_ENV', choices: 'ci\nbbb\nccc', description: 'Choose Env')
    }

    environment {
        bob2 = 'env | egrep -v "PATH|GERRIT_REFSPEC" > ${WORKSPACE}/env_var_bob; docker run --rm ' +
                '--env-file ${WORKSPACE}/env_var_bob -v ${WORKSPACE}/.docker/config.json:${HOME}/.docker/config.json ' +
                '-v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:${PWD} ' +
                '--workdir ${PWD} -u ${UID}:${GROUPS} -e HOME=${HOME} ' +
                'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:${BOB2_VERSION}'

        API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        BASE_HELMFILE = 'helmfile.yaml'
        DOCKER_CONFIG = 'CNAMFID_Docker_ARM'
        EVMS_API_KEY = credentials("cnamfid-evms-api-token-id")
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        NAMESPACE = 'ci-executor'
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        SONAR_TOKEN = credentials("cnamfid-sonar-token")
        BOB2_VERSION = '1.16.0-0'
    }

    stages {
        stage("Load environment variable") {
             steps {
             withCredentials([ file(credentialsId: "${env.DOCKER_CONFIG}", variable: 'DOCKER_ARM_CONFIG') ])
                    { writeFile file: './.docker/config.json', text: readFile(DOCKER_ARM_CONFIG) }
                   script {
                          def props = readProperties file: './Jenkins/k8s-env/' + params.K8_ENV + '.conf'
                          env.ENV_PROFILE_PRE = props.ENV_PROFILE_PRE
                          env.KUBERNETES_CONFIG_FILE_NAME = props.KUBERNETES_CONFIG_FILE_NAME
                          env.KUBERNETES_FQDN = props.KUBERNETES_FQDN
                   }
             }
        }
        stage('Preperation') {
            steps {
                withCredentials([
                   usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                 ]) {
                   sh "${bob2} preperation"
                }
            }
        }

        stage('Init PRECODE REVIEW') {
             steps {
                   withCredentials([
                     file(credentialsId: "${env.KUBERNETES_CONFIG_FILE_NAME}", variable: 'KUBE_CONF'),
                     file(credentialsId: "${env.REPOS_TOKEN}", variable: 'HELM_REPO_CREDENTIALS'),
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
                   ]) {
                        writeFile file: './.kube/config', text: readFile(KUBE_CONF)
                        writeFile file: './.bob/.helm-repositories.yaml', text: readFile(HELM_REPO_CREDENTIALS)
                        sh "${bob2} precode_init"
                   }
             }
        }
        stage ('Guides generation') {
            steps {
                  withCredentials([
                    usernamePassword(credentialsId: 'amadm100-password', usernameVariable: 'AMADM100_USER', passwordVariable: 'AMADM100_PASS')
                  ]) {
                         sh "${bob2} docs-generation"
                     }
            }
        }
        stage('Verify Dependency.yaml file') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'EVMS_USERNAME', passwordVariable: 'EVMS_PASSWORD')
                   ]) {
                         sh "${bob2} dependency_verify"
                      }
             }
        }
        stage ('Helm chart validation') {
            steps {
                  sh "${bob2} helm-dr-check"
            }
            post {
                 always {
                        archiveArtifacts '.bob/design-rule-check-report.html'
                 }
            }
        }
        stage('Compile source code') {
            steps {
                  sh "${bob2} compile-src"
            }
        }
        stage('Run unit tests and package jar file') {
            steps {
                  sh "${bob2} package-test"
            }
        }
        stage('Verify app source code with sonar') {
            steps {
                  sh "${bob2} sonar"
            }
        }
        stage('Creating docker image') {
            steps {
                 withCredentials([
                   usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                 ]) {
                       sh "${bob2} package-docker"
                    }
            }
        }
        stage('Publish Docker Image') {
            steps {
                 withCredentials([
                   usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                 ]) {
                       sh "${bob2} publish-dockerimage"
                    }
            }
            post {
                success {
                    script {
                        def DOCKER_IMAGE = readFile ".bob/var.dockerimage.artifactory"
                        summary.appendText("<li><b>Docker image: </b>docker pull ${DOCKER_IMAGE}</li>", false)
                        def DOCKER_INIT_IMAGE = readFile ".bob/var.dockerinitimage.artifactory"
                        summary.appendText("<li><b>Docker init image: </b>docker pull ${DOCKER_INIT_IMAGE}</li>", false)
                    }
                }
            }
        }
        stage('Creating helm chart') {
            steps {
               withCredentials([
                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
               ]) {
                     sh "${bob2} package-helm"
                  }
            }
        }
        stage('Publish snapshot helm chart') {
            steps {
               withCredentials([
                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
               ]) {
                     sh "${bob2} publish-helmchart"
                  }
            }
            post {
                success {
                    script {
                        def CHART_ARCHIVE = readFile ".bob/var.helmchart.artifactory"
                        summary.appendText("<li><b>Snapshot Helm Chart: </b><a href='${CHART_ARCHIVE}'>${CHART_ARCHIVE}</a></li>", false)
                    }
                }
            }
        }
        stage('Creating prerequisites: ClusterRoleBinding/CRD') {
             steps {
                   sh "${bob2} helmfile-RBAC-creation"
                   sh "${bob2} helm-RBAC-creation"
                   sh "${bob2} crd-install"
             }
        }
        stage('Helm/Helmfile parallel deployment') {
            parallel {
                stage('Helm') {
                    stages {
                        stage('Generating Integration Chart with snapshot app') {
                            steps {
                                withCredentials([
                                  usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                ]) {
                                    sh "${bob2} prepare-intchart"
                                }
                            }
                        }
                        stage('Deploy Integration Chart with snapshot') {
                            steps {
                                withCredentials([
                                  usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                ]) {
                                    sh "${bob2} deploy-chart"
                                }
                            }
                            post {
                                failure {
                                    sh "${bob2} logs-collector-helm"
                                    archiveArtifacts 'logs*.tgz'
                                }
                                success {
                                    script {
                                        def INTCHART_ARCHIVE = readFile ".bob/var.helmintchart.artifactory"
                                        summary.appendText("<li><b>Integration Helm Chart: </b><a href='${INTCHART_ARCHIVE}'>${INTCHART_ARCHIVE}</a></li>", false)
                                    }
                                }
                            }
                        }
                    }
                }
                stage('Helmfile') {
                    stages {
                        stage ('Deploy CN-APP with snapshot by Helmfile') {
                             steps {
                                    withCredentials([
                                      usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                    ]) {
                                         sh "${bob2} helmfile-deploy"
                                       }
                             }
                             post {
                                  failure {
                                          sh "${bob2} logs-collector-helmfile"
                                          archiveArtifacts 'logs*.tgz'
                                  }
                             }
                        }
                    }
                }
            }
        }
    }
    post {
         always {
                 script {
                  def AUTHOR = sh(returnStdout: true, script: "git log -1 --pretty=format:'%an'").trim()
                  def HASH = sh(returnStdout: true, script: "git log -1 --pretty=format:'%h'").trim()
                  manager.addShortText(AUTHOR, 'black', 'lightblue', '5px', 'white')
                  manager.addShortText(HASH, 'black', 'yellow', '5px', 'white')
                  allure([
                        reportBuildPolicy: 'ALWAYS',
                        results          : [[path: '**/allure-results']]
                  ])
                 }
                 sh "${bob2} helmfile-RBAC-deleting || echo 'Nothing to remove'"
                 sh "${bob2} helm-RBAC-deleting || echo 'Nothing to remove'"
                 sh "${bob2} remove-installed-release || echo 'Nothing to remove'"
                 cleanWs()
         }
    }
}
