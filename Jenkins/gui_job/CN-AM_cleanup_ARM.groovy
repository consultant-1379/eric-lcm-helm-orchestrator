/*
Build Environment -> Use secret text(s) or file(s)
Bindings -> Username and password (separated)
Username Variable -> ADMIN_USER
Password Variable -> ADMIN_PASSWORD
Credentials -> Specific credentials -> CNAM FUNC USER
 */
docker_reg="https://arm.seli.gic.ericsson.se/artifactory/docker-v2-global-local/proj-ra-cnam/eric-lcm-helm-executor/"
arm_ci_tmp="https://arm.sero.gic.ericsson.se/artifactory/proj-eric-lcm-helm-executor-artifacts-generic-local/CI/tmp"
helm_repo_int="https://arm.sero.gic.ericsson.se/artifactory/proj-bdgs-cn-app-mgmt-ci-internal-helm-local/eric-cn-app-mgmt-integration/"
helm_repo_executor="https://arm.sero.gic.ericsson.se/artifactory/proj-bdgs-cn-app-mgmt-ci-internal-helm-local/eric-lcm-helm-executor/"
helm_repo_iMS="https://arm.sero.gic.ericsson.se/artifactory/proj-bdgs-cn-app-mgmt-ci-internal-helm-local/eric-cims-integration/"
helm_repo_int_helmfile="https://arm.sero.gic.ericsson.se/artifactory/proj-bdgs-cn-app-mgmt-ci-internal-helm-local/eric-cn-app-mgmt-integration-helmfile"

echo "Clean up the snapshot images from docker registry"
docker_list=`curl $docker_reg | awk -F '"' '{print $2}' | grep -e "[0-9]-......."`
for i in $docker_list
do
curl -v -u$ADMIN_USER:$ADMIN_PASSWORD -X DELETE $docker_reg$i
done

echo "Clean up the snapshot images from helm repo"

for i in $helm_repo_int $helm_repo_executor $helm_repo_helmfile $helm_repo_iMS $helm_repo_int_helmfile $arm_ci_tmp
do
curl -v -u$ADMIN_USER:$ADMIN_PASSWORD -X DELETE $i
done

echo "test" > test.txt
curl -sSf -u$ADMIN_USER:$ADMIN_PASSWORD -X PUT -T test.txt $helm_repo_int_helmfile/test.txt