String patch_set = params.GERRIT_PATCHSET
String[] changes = patch_set.split('/')
println(patch_set)
println(changes[5])

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
        MVN_OPTIONS = ' '
        NAMESPACE = 'manual-test'
        BASE_HELMFILE = 'helmfile.yaml'
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
        PIPELINE_VALUES_FILE_PATH = './Jenkins/ci-values.yaml'
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        BOB2_VERSION = '1.16.0-0'
    }

    stages {

        stage("Download the patch-set") {
            steps {
                script{
                    def String[] refspec = sh(returnStdout: true, script: "ssh -p 29418 gerrit.ericsson.se gerrit query --current-patch-set --format=TEXT change:" +changes[5]+ " | grep ref").split(':')
                    //env.REFSPEC = refspec[1]
                    sh 'git init && git fetch ssh://gerrit.ericsson.se:29418/OSS/com.ericsson.orchestration.mgmt/eric-lcm-helm-orchestrator' + refspec[1]
                    sh 'git checkout FETCH_HEAD'
                }

            }
        }
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
        stage('env prep') {
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
                    sh '''
                           curl -u ${GERRIT_USERNAME}:${GERRIT_PASSWORD} -X POST https://arm.epk.ericsson.se/artifactory/api/search/aql -H "content-type: text/plain" -d 'items.find({ "repo": {"$eq":"docker-v2-global-local"}, "path": {"$match" : "proj-ldc/common_base_os_release/*"}}).sort({"$desc": ["created"]}).limit(1)' 2>/dev/null | grep path | awk -F '/' '{print $4}' | sed 's/",//'>.bob/var.cbo-version
                           '''
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
        stage('Creating prerequisites: ClusterRoleBinding/CRD') {
            steps {
                sh "${bob2} helmfile-RBAC-creation"
            }
        }
        stage ('Helmfile deployment') {
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
                    sh "./scripts/collect_adp_logs.sh sftp .kube/config"
                    archiveArtifacts 'logs*.tgz'
                }
            }
        }
        stage('Helmfile Robustness testing DB') {
            when { environment name: 'enable_robustness_test', value: 'true' }
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

    }
    post {
        always {
            sh "${bob2} helmfile-RBAC-deleting || echo 'Nothing to remove'"
            sh "${bob2} remove-installed-release || echo 'Nothing to remove'"
            sh "${bob2} remove-installed-HExLW || echo 'Nothing to remove'"
            cleanWs()
        }
    }
}