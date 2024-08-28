def summary = manager.createSummary("folder.gif")
summary.appendText("Additional atrifacts:", false)


def robustness_db_test_1() {
    stages = [:]
    stages["Test Execution(robustness).1"] = { stage('Test Execution(DB) 1') {
        sh "${bob2} acceptance-test"
    }
    }
    stages["Test Execution(robustness).2"] = { stage('DB Stress flow 1') {
        sh "sleep 300; ${bob2} robustness-db-1"
    }
    }
    return stages
}


def robustness_db_test_2() {
    stages = [:]
    stages["Test Execution(robustness).1"] = { stage('Test Execution(DB) 2') {
        sh "${bob2} acceptance-test"
    }
    }
    stages["Test Execution(robustness).2"] = { stage('DB Stress flow 2') {
        sh "sleep 500; ${bob2} robustness-db-2"
    }
    }
    return stages
}


def robustness_hcr_test_1() {
    stages = [:]
    stages["Test Execution(robustness).1"] = { stage('Test Execution(HCR) 1') {
        sh "${bob2} acceptance-test"
    }
    }
    stages["Test Execution(robustness).2"] = { stage('HCR Stress flow 1') {
        sh "sleep 300; ${bob2} robustness-hcr-1"
    }
    }
    return stages
}


def robustness_hcr_test_2() {
    stages = [:]
    stages["Test Execution(robustness).1"] = { stage('Test Execution(HCR) 2') {
        sh "${bob2} acceptance-test"
    }
    }
    stages["Test Execution(robustness).2"] = { stage('HCR Stress flow 2') {
        sh "sleep 500; ${bob2} robustness-hcr-2"
    }
    }
    return stages
}

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
                '--env-file ${WORKSPACE}/env_var_bob ' +
                '-v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:${PWD} ' +
                '--workdir ${PWD} -u ${UID}:${GROUPS} ' +
                'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:${BOB2_VERSION}'

        NAMESPACE = 'night-tests'
        BASE_HELMFILE = 'helmfile.yaml'
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
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

        stage('Init PRECODE REVIEW') {
            steps {
                withCredentials([
                        file(credentialsId: "${env.KUBERNETES_CONFIG_FILE_NAME}", variable: 'KUBE_CONF'),
                        file(credentialsId: "${env.REPOS_TOKEN}", variable: 'HELM_REPO_CREDENTIALS'),
                        usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
                ]) {
                    sh "[ -d ${env.WORKSPACE}/.kube/ ] || mkdir ${env.WORKSPACE}/.kube/"
                    sh "cp \$KUBE_CONF ${env.WORKSPACE}/.kube/config"
                    sh "[ -d ${env.WORKSPACE}/.bob/ ] || mkdir ${env.WORKSPACE}/.bob/"
                    sh "cp \$HELM_REPO_CREDENTIALS .bob/.helm-repositories.yaml"
                    sh "${bob2} precode_init"
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

        stage('Helmfile deployment') {
            steps {
                withCredentials([
                        usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                ]) {
                    sh "${bob2} acceptance-tests-helmfile-deploy"
                }
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Functional testing') {
            steps {
                sh "${bob2} acceptance-test"
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Robustness testing DB') {
            when { expression { return false }}
            steps {
                script { parallel robustness_db_test_1() }
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Robustness testing HCR') {
            when { expression { return false }}
            steps {
                script { parallel robustness_hcr_test_1() }
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Robustness testing DB stage 2') {
            when { expression { return false }}
            steps {
                script { parallel robustness_db_test_2() }
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Robustness testing HCR stage 2') {
            when { expression { return false }}
            steps {
                script { parallel robustness_hcr_test_2() }
            }
            post {
                failure {
                    sh "${bob2} logs-collector-helmfile"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }

    }

    post {
         failure {
            mail to: "pdlsunflow@pdl.internal.ericsson.com",
                    subject: "${currentBuild.fullDisplayName} failed",
                    body: "${env.BUILD_URL}",
                    from: "E2E-tests-result@ericsson.com"
         }
         aborted {
            mail to: "pdlsunflow@pdl.internal.ericsson.com",
                    subject: "${currentBuild.fullDisplayName} failed",
                    body: "${env.BUILD_URL}",
                    from: "E2E-tests-result@ericsson.com"
         }
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
                 sh "${bob2} remove-installed-HExLW || echo 'Nothing to remove'"
                 cleanWs()
         }
    }
}
