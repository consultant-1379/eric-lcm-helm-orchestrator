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
        choice(name: 'K8_ENV', choices: 'hahn117\nbbb\nccc', description: 'Choose Env')
    }

    environment {
        bob2 = 'env | egrep -v "PATH|GERRIT_REFSPEC" > ${WORKSPACE}/env_var_bob; docker run --rm ' +
                '--env-file ${WORKSPACE}/env_var_bob ' +
                '-v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:${PWD} ' +
                '--workdir ${PWD} -u ${UID}:${GROUPS} ' +
                'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:${BOB2_VERSION}'

        EVMS_API_KEY = credentials("cnamfid-evms-api-token-id")
        NAMESPACE = 'drop-executor'
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
        SONAR_TOKEN = credentials("cnamfid-sonar-token")
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        BOB2_VERSION = '1.7.0-78'
    }

    stages {
        stage("Load environment variable") {
             steps {
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

        stage('Init RELEASE') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} release_init"
                         withCredentials([
                           file(credentialsId: "${env.KUBERNETES_CONFIG_FILE_NAME}", variable: 'KUBE_CONF'),
                           file(credentialsId: "${env.REPOS_TOKEN}", variable: 'HELM_REPO_CREDENTIALS')
                         ]) {
                              sh "[ -d ${env.WORKSPACE}/.kube/ ] || mkdir ${env.WORKSPACE}/.kube/"
                              sh "cp \$KUBE_CONF ${env.WORKSPACE}/.kube/config"
                              sh "[ -d ${env.WORKSPACE}/.bob/ ] || mkdir ${env.WORKSPACE}/.bob/"
                              sh "cp \$HELM_REPO_CREDENTIALS .bob/.helm-repositories.yaml"
                         }
                      }
             }
        }

        stage('Generate preliminary PRI document') {
            steps
            {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'JIRA_USERNAME', passwordVariable: 'JIRA_PASSWORD'),
                                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')
                                ])
                {
                    sh "${bob2} generate-preliminary-pri"
                    publishHTML (target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'marketplace/pri',
                        reportFiles: 'pri.html',
                        reportName: "PRI"
                    ])
                }
           }
        }

        stage('Guides generation') {
             steps {
                    withCredentials([
                      usernamePassword(credentialsId: 'amadm100-password', usernameVariable: 'AMADM100_USER', passwordVariable: 'AMADM100_PASS')
                    ]) {
                          sh "${bob2} docs-generation"
                       }
            }
        }

          stage('Update product info') {
             steps {
                   sh "${bob2} update-product-info"
            }
        }

        stage('Helm chart validarion') {
             steps {
                   sh "${bob2} helm-dr-check"
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
                   catchError(buildResult: 'UNSTABLE', catchInterruptions: false, stageResult: 'FAILURE') {
                    sh "${bob2} sonar"
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

       stage('Creating docker image') {
            steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} package-docker"
                      }
            }
        }

        stage('Publish release helm chart') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} publish-helmchart"
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
        }

        stage('Archive artifact artifact.properties') {
             steps {
                   archiveArtifacts 'artifact.properties'
             }
        }

        stage('Generating Integration Chart with Application') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                       sh "${bob2} prepare-intchart"
                   }
             }
        }

        stage('Creating prerequisites: ClusterRoleBinding/CRD') {
             steps {
                   sh "${bob2} helm-RBAC-creation"
                   sh "${bob2} crd-install"
             }
        }

        stage('Deploy Integration Chart') {
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
                          archiveArtifacts 'logs_eric*.tgz'
                  }
             }
        }

        stage('Publish Guides') {
             steps {
                   sh "${bob2} marketplace-upload"
                   sh "curl -X POST -H 'Accept: application/json' https://adp.ericsson.se/api/integration/v1/microservice/documentrefresh?access_token=${env.MARKETPLACE_TOKEN}"
            }
        }

        stage('Upload documents to Eridoc'){
            steps {
                    withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                 {
                     sh "${bob2} eridoc-upload"
                 }
            }
        }

        stage('Publish jars to nexus') {
            steps {
                   withCredentials([
                     usernamePassword(credentialsId: "db2f275c-c8c5-446c-82a6-2253d149af71", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} publish-jar-cred"
                         sh "${bob2} publish-jar"
                      }
            }
        }
        stage('Push commit to gerrit repo') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} push-commit-update"
                      }
             }
        }
//        stage('Set git tag for drop version') {
//             steps {
//                   withCredentials([
//                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
//                   ]) {
//                         sh "${bob2} drop-tag"
//                      }
//             }
//        }
        stage('Update product version in Munin') {
             steps {
                    sh "${bob2} munin-update-version"
             }
        }
        stage('EVMS Pre-registration') {
             steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'EVMS_USERNAME', passwordVariable: 'EVMS_PASSWORD')
                   ]) {
                         sh "${bob2} evms-pre-registration"
                      }
             }
        }
    }
    post {
         failure {
                  mail to: "pdlsunflow@pdl.internal.ericsson.com",
                       subject: "${currentBuild.fullDisplayName} failed",
                       body: "${env.BUILD_URL}",
                       from: "cnam-jenkins.noreply@ericsson.com"
                  sh "${bob2} helm-RBAC-deleting || echo 'Nothing to remove'"
                  sh "${bob2} remove-installed-release || echo 'Nothing to remove'"
                  cleanWs()
         }
         aborted {
                  mail to: "pdlsunflow@pdl.internal.ericsson.com",
                       subject: "${currentBuild.fullDisplayName} failed",
                       body: "${env.BUILD_URL}",
                       from: "cnam-jenkins.noreply@ericsson.com"
                  sh "${bob2} helm-RBAC-deleting || echo 'Nothing to remove'"
                  sh "${bob2} remove-installed-release || echo 'Nothing to remove'"
                  cleanWs()
         }
         success {
                  cleanWs()
         }
    }
}
