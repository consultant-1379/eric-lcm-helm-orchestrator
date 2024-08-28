env.REPOS_TOKEN = 'repository-tokens-file'
env.API_TOKEN = 'credentials("sero-artifactory-api-token-id")'
env.MARKETPLACE_TOKEN = 'credentials("helm-executor-marketpalce-token-id")'
env.GERRIT_ID = 'db2f275c-c8c5-446c-82a6-2253d149af71'
env.KUBERNETES_CONFIG_FILE_NAME = 'ccd-hahn117.kube.conf'
env.BOB2_VERSION = '1.7.0-78'
env.bob2 = "docker run --rm " +
        '--workdir "`pwd`" ' +
                '--env ENV_PROFILE_PRE=${ENV_PROFILE_PRE} ' +
                '--env BASE_HELMFILE=${BASE_HELMFILE} ' +
                '--env JENKINS_URL=${JENKINS_URL} ' +
                '--env API_TOKEN=${API_TOKEN} ' +
                '--env MARKETPLACE_TOKEN=${MARKETPLACE_TOKEN} ' +
                '--env JOB_NAME=${JOB_NAME} ' +
                '--env BUILD_NUMBER=${BUILD_NUMBER} ' +
                '--env GERRIT_USERNAME=${GERRIT_USERNAME} ' +
                '--env GERRIT_PASSWORD=${GERRIT_PASSWORD} ' +
                '--env CHART_REPO=${CHART_REPO} ' +
                '--env CHART_VERSION=${CHART_VERSION} ' +
                '--env CHART_NAME=${CHART_NAME} ' +
        '-u $(id -u ${USER}):$(id -g ${USER}) ' +
        "-w `pwd` " +
        "-v \"`pwd`:`pwd`\" " +
        "-v /var/run/docker.sock:/var/run/docker.sock " +
        "armdocker.rnd.ericsson.se/sandbox/adp-staging/adp-cicd/bob.2.0:${BOB2_VERSION}"
===========


GERRIT_ID = 'cdbc7c5b-9a86-4074-a15e-3e6558998860'
FOSS_TOKEN = credentials("cnamfid-foss-prod-token")
MUNIN_TOKEN = credentials("cnamfid-munin-token")
REPOS_TOKEN = 'cnamfid-repository-tokens-file'
MARKETPLACE_TOKEN = credentials("helm-executor-marketpalce-token-id")
API_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
EVMS_API_KEY = credentials("cnamfid-evms-api-token-id")
HELM_TOKEN = credentials("cnamfid-sero-artifactory-api-token-id")
ARMDOCKER_TOKEN = credentials("cnamfid-seli-artifactory-api-token-id")
