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
        string(name: 'CHART_VERSION', defaultValue: '0.7.10-42')
    }

    environment {
        bob2 = 'env | egrep -v "PATH|GERRIT_REFSPEC" > ${WORKSPACE}/env_var_bob; docker run --rm ' +
                '--env-file ${WORKSPACE}/env_var_bob ' +
                '-v /var/run/docker.sock:/var/run/docker.sock -v ${PWD}:${PWD} ' +
                '--workdir ${PWD} -u ${UID}:${GROUPS} ' +
                'armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob.2.0:${BOB2_VERSION}'

        NAMESPACE = 'ci-executor'
        BASE_HELMFILE = 'helmfile.yaml'
        MUNIN_TOKEN = credentials("cnamfid-munin-token")
        BAZAAR_TOKEN = credentials("cnamfid-bazaar-token")
        REPOS_TOKEN = 'cnamfid-repository-tokens-file'
        FOSS_TOKEN = credentials("cnamfid-foss-prod-token")
        API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
        MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
        GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
        BOB2_VERSION = '1.16.0-0'
    }

    stages {
        stage('Analyze') {
             steps {
                   sh "${bob2} fossa-deps"
                   sh "${bob2} fossa-analyze"
             }
        }

        stage('Get Dependencies Report') {
             steps {
                   sh "${bob2} fossa-scan-status-check"
                   sh "${bob2} fossa-report"
                   archiveArtifacts '.bob/foss_report.json'
             }
        }

        stage('Get Vulnerability Report') {
             steps {
                   sh "${bob2} fetch-vulnerability-report"
                   archiveArtifacts '.bob/foss-issues.json'
             }
        }

        stage('Check new dependencies file with Bazaar') {
             steps {
                   withCredentials([usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'BAZAAR_USER', passwordVariable: 'BAZAAR_PASSWORD')]) {
                       catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                           sh "${bob2} dependency-update"
                           sh "${bob2} dependency-update-with-bazaar"
                       }
                   }
                   archiveArtifacts 'dependencies-3pp.yaml'
             }
        }

        stage('Validate Dependencies') {
            steps {
                  catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                      sh "${bob2} dependency-validate"
                  }
            }
       }

       stage('Validate Mimer Product versions') {
           steps {
                 catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                     sh "${bob2} bazaar-check"
                 }
           }
       }

//       stage('Generate Dependencies HTML Report') {
//            steps {
//                  sh "${bob2} dependency-report"
//            }
//       }

       stage('License Agreement Generate') {
           steps {
                 catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                     sh "${bob2} license-agreement-generate"
                 }
                 archiveArtifacts 'ci_config/fragments/license.agreement.json'
           }
       }

//       stage('License Agreement Validate') {
//           steps {
//                 sh "${bob2} license-agreement-validate"
//           }
//       }

//        stage('Push new dependency.image.yaml to gerrit') {
//             steps {
//                   withCredentials([
//                     usernamePassword(credentialsId: "${env.GERRIT_ID}", usernameVariable: 'GERRIT_USERNAME', passwordVariable: 'GERRIT_PASSWORD'),
//                   ]) {
//                         sh "${bob2} push-dependency"
//                      }
//             }
//        }
    }
    post {
         always {
             script {
                  manager.addShortText(CHART_VERSION, 'black', 'lightblue', '5px', 'white')
             }
             cleanWs()
         }
    }
}

