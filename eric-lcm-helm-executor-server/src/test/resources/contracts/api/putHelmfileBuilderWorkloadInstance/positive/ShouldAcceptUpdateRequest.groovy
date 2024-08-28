/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package contracts.api.putHelmfileBuilderWorkloadInstance.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Updating a Workload Instance via Helmfile Builder. Updating means scale, upgrade, rollback, etc

```
given:
  client requests to update a Workload instance with Helmfile Builder.
when:
  a valid request is submitted
then:
  the Workflow instance is updated
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/helmfile_builder/workload_instances/some_valid_id"
        multipart(
                workloadInstanceWithChartsPutRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status ACCEPTED()
        body(
                """
                       {
                        "workloadInstanceId": "testId",
                        "workloadInstanceName": "testName",
                        "namespace": "testNamespace",
                        "cluster": "testCluster"
                       }
                """
        )
        headers {
            header(location(), "http://localhost/cnwlcm/v1/operations/test-operation-id")
        }
    }
}

