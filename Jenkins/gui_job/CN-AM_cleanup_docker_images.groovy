//##Build Triggers
//Build periodically
//Schedule:
//
//    H 1 * * *
//##Configuration Matrix
//Agents
//
//Name : ByNodes
//Node/Label:
//
//    process-engine-01
//    process-engine-02
//    process-engine-03

docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-py3kubehelmbuilder:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/adp-int-helm-chart-auto:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-adp-release-auto:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/anchore-inline-scan:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-docbuilder:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/sandbox/adp-staging/adp-cicd/common-library-adp-helm-dr-check:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/bob-java11mvnbuilder:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/va-scan-kubeaudit:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/va-scan-kubebench:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/va-scan-kubehunter:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/va-scan-kubesec:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/trivy-inline-scan:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/defensics.cbo:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/anchore-inline-scan:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/trivy-inline-scan:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-adp-cicd-drop/adp-int-helm-chart-auto:latest || echo 'Nothing to cleamup'
docker rmi -f armdocker.rnd.ericsson.se/proj-ra-cnam/acceptance-test-image:latest || echo 'Nothing to cleamup'
docker rmi -f `docker images -q armdocker.rnd.ericsson.se/proj-ra-cnam/eric-lcm-helm-executor` || echo 'Nothing to cleamup'
docker system prune --force