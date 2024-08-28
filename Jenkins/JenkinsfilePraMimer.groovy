pipeline
{
    agent
    {
        node
        {
            label 'cnam'
        }
    }
    parameters
    {
        string(name: 'BRANCH_NAME', defaultValue: 'master')
        string(name: 'RELEASE_CANDIDATE', defaultValue: '0.3.3-1')
        choice(name: 'VERSION_UPDATE', choices: ['MINOR', 'PATCH', 'MAJOR'])
        booleanParam(name: 'DRY_RUN', defaultValue: true)
    }
    environment {
        bob2 =  'env | egrep -v "PATH|GERRIT_REFSPEC" > ${WORKSPACE}/env_var_bob; docker run --rm ' +
                '--env-file ${WORKSPACE}/env_var_bob -v ${WORKSPACE}/.docker/config.json:${HOME}/.docker/config.json ' +
                '-v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:${PWD} ' +
                '--workdir ${PWD} -u ${UID}:${GROUPS} ' +
                'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:${BOB2_VERSION}'

        ARMDOCKER_TOKEN = credentials("cnamfid-seli-artifactory-api-token-id")
        DOCKER_CONFIG = 'CNAMFID_Docker_ARM'
        EVMS_API_KEY = credentials("cnamfid-evms-api-token-id")
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        HELM_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        RELEASE_JOB_NAME = 'CN-AM_Lcm-Helm-Executor_RELEASE'
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        VHUB_API_TOKEN = credentials("cnamfid-vhub-token")
        BOB2_VERSION = '1.16.0-0'
    }

    stages {
        stage('Cleanup') {
            steps {
                withCredentials([
                        file(credentialsId: "${env.DOCKER_CONFIG}", variable: 'DOCKER_ARM_CONFIG')
                ]) {
                    writeFile file: './.docker/config.json', text: readFile(DOCKER_ARM_CONFIG)
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml clean"
                }
            }
        }

        stage('Init') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml init"
                archiveArtifacts 'artifact.properties'
            }
        }

        stage('Publish released Docker Images') {
            steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                   ]) {
                         sh "${bob2} -r ruleset2.0.pra.mimer.yaml publish-released-docker-image"
                      }
            }
        }

        stage('Publish released helm chart') {
            steps {
                   withCredentials([
                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
                   ]) {
                         sh "${bob2} -r ruleset2.0.pra.mimer.yaml publish-released-helm-chart"
                      }
            }
        }

        stage('Fetch artifact checksums') {
            steps {
                withCredentials([
                  usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
                ])
                  {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml fetch-artifact-checksums"
                  }
            }
        }
        stage('Store artifacts to Mimer') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')
                ])
                {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml archive-artifacts"
                }
            }
        }
        stage('Generate document list') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD'),
                                 usernamePassword(credentialsId: 'amadm100-password', usernameVariable: 'AMADM100_USER', passwordVariable: 'AMADM100_PASS')]) {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml generate-document-list"
                }
            }
        }
        stage('Generate PRI') {
            steps
            {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'JIRA_USERNAME', passwordVariable: 'JIRA_PASSWORD'),
                                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
                                 usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')
                                ])
                {
                    // NOTE! ignore failure here for testing purposes, remove it once when job is stable
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml generate-pri || echo 'Documents are already uploaded and approved'"
                    publishHTML (target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'marketplace/pri',
                        reportFiles: 'pri.html',
                        reportName: "PRI"
                    ])
                    archiveArtifacts 'ci_config/plms/documents.yaml'
                    archiveArtifacts 'build/**'
                    archiveArtifacts 'marketplace/pri/**'
                }
           }
        }

        stage('Release product in Munin') {
            steps
                {
                  withCredentials([
                    usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'ERIDOC_USERNAME', passwordVariable: 'ERIDOC_PASSWORD')
                  ])
                    {
                      sh "${bob2} -r ruleset2.0.pra.mimer.yaml generate-document-list"
                    }
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml munin-release-version"
            }
        }

        stage('Generate Characteristics Report') {
            steps {
                   sh "${bob2} -r ruleset2.0.pra.mimer.yaml generate-characteristics-report"
                   archiveArtifacts 'marketplace/characteristics_report.html'
            }
        }

        stage('Marketplace Documentation') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml generate-doc-zip-package"
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml marketplace-upload"
            }
        }

        stage('Load documents in Eridoc') {
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                        {
                            sh "${bob2} -r ruleset2.0.pra.mimer.yaml eridoc-load"
                        }
            }
        }

        stage('Approve documents in Eridoc') {
            when { expression { env.DRY_RUN == "false" }}
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                        {
                            sh "${bob2} -r ruleset2.0.pra.mimer.yaml eridoc-approve"
                        }
            }
        }

        stage('Upload CPI fragment') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml upload-cpi-fragment"
            }
        }

        stage('Structure Data') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml structure-data-generate"
                archiveArtifacts 'build/structure-output/eric-lcm-helm-executor-structured-data.json'
            }
        }

        stage('EVMS Registration') {
            steps {
                  withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                   {
                     sh "${bob2} -r ruleset2.0.pra.mimer.yaml evms-registration"
                   }
                 }
        }

        stage('VHUB Set Release') {
            steps {
                catchError(buildResult: 'UNSTABLE', catchInterruptions: false, stageResult: 'FAILURE') {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml vhub-set-release"
                }
            }
        }

        stage('Create PRA Git Tag') {
            when { expression { env.DRY_RUN == "false" }}
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml create-pra-git-tag"
                }
            }
        }

        stage('Increment version prefix') {
            when { expression { env.DRY_RUN == "false" }}
            steps {
                withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD')])
                {
                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml increment-version-prefix"
                }
            }
        }

        stage('Create PRIM bookmark') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml munin-create-bookmarks"
            }
        }

        stage('Design Rules Checkers') {
            parallel {
                stage('PLM checker') {
                    stages {
                        stage('Start PLM check') {
                            when { expression { env.DRY_RUN == "false" }}
                            steps {
                                withCredentials([
                                  usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'PLMS_USERNAME', passwordVariable: 'PLMS_PASSWORD'),
                                ]) {
                                       sh "${bob2} -r ruleset2.0.pra.mimer.yaml plm-checker"
                                }
                            }
                            post {
                                always {
                                    publishHTML (target: [
                                        allowMissing: false,
                                        alwaysLinkToLastBuild: true,
                                        keepAll: true,
                                        reportDir: 'build/',
                                        reportFiles: 'plm_check_report.html',
                                        reportName: "PLMS DR Report"
                                    ])
                                    archiveArtifacts 'build/plm_check_report.html'
                                }
                            }
                        }
                    }
                }
                stage('Marketplace checker') {
                    when { expression { env.DRY_RUN == "false" }}
                    stages {
                        stage ('Start Marketplace check') {
                            steps {
                                catchError(buildResult: 'UNSTABLE', catchInterruptions: false, stageResult: 'FAILURE') {
                                    sh "${bob2} -r ruleset2.0.pra.mimer.yaml marketplace-checker"
                                }
                            }
                            post {
                                success {
                                    publishHTML (target: [
                                        allowMissing: false,
                                        alwaysLinkToLastBuild: true,
                                        keepAll: true,
                                        reportDir: 'build/',
                                        reportFiles: 'marketplace_check_report.html',
                                        reportName: "Marketplace DR Report"
                                    ])
                                    archiveArtifacts 'build/marketplace_check_report.html'
                                }
                            }
                        }
                    }
                }
                stage('Artifacs Rule Checker') {
                    when { expression { env.DRY_RUN == "false" }}
                    stages {
                        stage ('Start Artifacts Rule Checker') {
                            steps {
                                sh "${bob2} -r ruleset2.0.pra.mimer.yaml artifact-dr-checker"
                            }
                            post {
                                success {
                                    publishHTML (target: [
                                        allowMissing: false,
                                        alwaysLinkToLastBuild: true,
                                        keepAll: true,
                                        reportDir: 'build/',
                                        reportFiles: 'marketplace_check_report.html',
                                        reportName: "Artifacts DR Report"
                                    ])
                                    archiveArtifacts 'artifact_dr_check_report.html'
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Send Release Email') {
            when { expression { env.DRY_RUN == "false" }}
            steps {
                script {
                    def exists = fileExists 'marketplace/pri/pra_release_email.html'
                    if (exists) {
                        message = readFile('marketplace/pri/pra_release_email.html')
                        VERSION = sh(returnStdout: true, script: 'cat .bob/var.semver').trim()
                        mail body: message, subject:"Helmfile Executor - $VERSION, PRA Release", to: 'pdlsunflow@pdl.internal.ericsson.com,adp-interest@openalm.lmera.ericsson.se,himansu.nayak@ericsson.com,vinicius.garcia.ext@ericsson.com,rob.gerrard@ericsson.com,earl.gaylard@ericsson.com,sundaresan.balasubramanian@ericsson.com,andreas.torstensson@ericsson.com', mimeType: 'text/html'
                    } else {
                        echo 'No release contents found for notifying. Email sending aborted...'
                    }
                }
            }
        }
        stage('Final Cleanup') {
            steps {
                sh "${bob2} -r ruleset2.0.pra.mimer.yaml clean"
            }
        }
        stage('Cleaning the Jenkins Release job history') {
            when { expression { env.DRY_RUN == "false" }}
            steps {
                script{
                    def item = Jenkins.instance.getItem("${RELEASE_JOB_NAME}")
                    item.builds.each() { build -> build.delete() }
                    item.nextBuildNumber = 1
                    item.save()
                }
            }
        }
    }
    post {
        success {
            cleanWs()
        }
        always {
            archiveArtifacts 'build/**'
            sh "${bob2} -r ruleset2.0.pra.mimer.yaml cleanup-images"
        }
//        failure {
//            mail to: 'pdlsunflow@pdl.internal.ericsson.com',
//                 subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
//                 body: "Failure on ${env.BUILD_URL}"
//        }
    }
}
