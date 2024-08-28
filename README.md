# Helmfile Executor

The primary goal of the Helmfile Executor service is to apply helmfile.tgz files to a target cluster and execute 
installations of Helm releases on the cluster across one or more namespaces.

##Documentation:
For more detailed information about the architecture and the REST API specifications of the Helmfile Executor, please refer to 
[user guide](marketplace/user_guide/user_guide_template.md) and [developer guide](marketplace/developers_guide/developers_guide_template.md).

### acceptance test module

This repository has a module of tests which can be run against a deployed system.

It is under a separate mvn profile, acceptance.

It requires 3 input properties.

1. the suite File to execute
2. the ip of the service
3. the port of the service

This service does not have an ingress, it can be accessed externally by setting the kubernetes service object type to
 NodePort and retrieving the expose port using the `kubectl get service <name>` command
 The IP is one of the worker node external IPs.

```bash

mvn clean install -Pacceptance -Dsurefire.suiteXmlFiles=src/main/resources/suites/helmfilelcm.xml -Daccess.ip=localhost -Daccess.port=8080


```

Currently, all test Data is stored internally in the maven module, however the plan is to allow this to be external too.

For local usage, please use command with flag "isLocal". This flag means the logic is switch on proper for local testing 
clusterConnectionInfo file 

```bash

mvn clean install -Pacceptance -Dsurefire.suiteXmlFiles=src/main/resources/suites/helmfilelcm.xml -Daccess.ip=localhost -Daccess.port=8080 -DisLocal=true


```

## Contributing
We are an inner source project and welcome contributions. See our [contribution guide](CONTRIBUTING.md) for details.

##Contacts 
If you have any questions or need further assistance, please feel free to reach out:
-  **Sunflowers team:** PDLSUNFLOW@pdl.internal.ericsson.com
-  **Product owner:** illia.korchan.ext@ericsson.com