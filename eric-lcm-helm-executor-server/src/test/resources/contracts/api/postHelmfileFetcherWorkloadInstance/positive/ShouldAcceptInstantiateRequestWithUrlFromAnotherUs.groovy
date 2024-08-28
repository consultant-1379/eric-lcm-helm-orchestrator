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

package contracts.api.postHelmfileFetcherWorkloadInstance.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Instantiating a Workload Instance via Helmfile Fetcher

```
given:
  client requests to instantiate a Workload instance via Helmfile Fetcher
when:
  a valid request is submitted
then:
  the Workflow instance is instantiated
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/helmfile_fetcher/workload_instances"
        multipart(
                workloadInstanceWithURLRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-json-part.json'))),
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

