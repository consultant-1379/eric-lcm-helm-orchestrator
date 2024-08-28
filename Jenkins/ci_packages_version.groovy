pipeline {
    options {
        disableConcurrentBuilds()
    }

    agent {
        node {
            label 'cnam'
        }
    }

    environment {
        URL_ARM_REPO = "https://arm.sero.gic.ericsson.se/artifactory/proj-eric-lcm-helm-executor-artifacts-generic-local/"
        // Packages
        URL_HELM_REPO = "https://get.helm.sh/helm-${HELM_VERSION}-linux-amd64.tar.gz"
        URL_HELMFILE = "https://github.com/helmfile/helmfile/releases/download/${HELMFILE_VERSION}/helmfile_${HELMFILE_VERSION//v}_linux_amd64.tar.gz"
        // Plugins
        URL_HELM_DIFF = "https://github.com/databus23/helm-diff/releases/download/${HELM_DIFF_VERSION}/helm-diff-linux-amd64.tgz"
        URL_HELM_GIT = "https://github.com/aslafy-z/helm-git/archive/refs/tags/${HELM_GIT_VERSION}.tar.gz"
//                URL_INOTIFY_TOOLS = 'https://mirrorcache.opensuse.org/repositories/filesystems/SLE_12_SP5/x86_64/'
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'

    }
    stages {
        stage("Download packages from source repo") {
            steps {
                script {
                    if (params.HELM == true) {
                        sh 'wget ${URL_HELM_REPO}'
                    }
                    if (params.HELM_DIFF == true) {
                        sh 'wget ${URL_HELM_DIFF}'
                    }
                    if (params.HELMFILE == true) {
                        sh 'wget ${URL_HELMFILE}'
                    }
//                        if (params.INOTIFY_TOOLS == true) {
//                            INOTIFY_TOOLS_VERSION
//                        }
                }
            }
        }

        stage('Upload packeges to ARM') {
            steps {
                script {
                    withCredentials([
                            usernamePassword(credentialsId: "${GERRIT_ID}", usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')
                    ]) {
                        if (params.HELM == true) {
                            sh 'curl -sSf -u$USERNAME:$PASSWORD -X PUT -T ./helm-${HELM_VERSION}-linux-amd64.tar.gz ${URL_ARM_REPO}packages/helm/${HELM_VERSION}/'
                        }
                        if (params.HELM_DIFF == true) {
                            sh 'curl -sSf -u$USERNAME:$PASSWORD -X PUT -T ./helm-diff-linux-amd64.tgz ${URL_ARM_REPO}plugins/helm-diff/${HELM_DIFF_VERSION}/'
                        }
                        if (params.HELM_GIT == true) {
                            sh 'curl -sSf -u$USERNAME:$PASSWORD -X PUT -T ./${HELM_GIT_VERSION}.tar.gz ${URL_ARM_REPO}plugins/helm-git/${HELM_GIT_VERSION}/'
                        }
                        if (params.HELMFILE == true) {
                            sh 'curl -sSf -u$USERNAME:$PASSWORD -X PUT -T ./helmfile_${HELMFILE_VERSION//v}_linux_amd64.tar.gz ${URL_ARM_REPO}packages/helmfile/${HELMFILE_VERSION}/helmfile_${HELMFILE_VERSION}_linux_amd64.tar.gz'
                        }
// curl -sSf -u$USERNAME:$PASSWORD -X PUT -T ./inotify-tools-3.20.2.2-17.1.x86_64.rpm ${URL_ARM_REPO}inotify-tools/

                    }
                }
            }
        }

//                stage('Uplift packages in code') {
//                    steps {
//                        catchError(buildResult: 'UNSTABLE', catchInterruptions: false, stageResult: 'FAILURE') {
//                            sh "${bob2} anchore-scan"
//                            sh "mv anchore_metadata.properties anchore-reports/; mv anchore-reports ./va_reports/"
//                        }
//                    }
//                }
    }

    post {
        always {
            cleanWs()
        }
    }
}

