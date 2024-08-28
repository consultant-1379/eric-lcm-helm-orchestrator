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

package contracts.api.postInstantiateWorkload.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Instantiating a Workload Instance

```
given:
  client requests to instantiate a Workload instance with Helm File
when:
  a valid request is submitted
then:
  the Workflow instance is instantiated
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/workload_instances"
        multipart(
                workloadInstancePostRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-json-part.json'))),
                        contentType: value("application/json")
                ),
                helmSource: named(
                        name: value('helmFile.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('helmFile.yaml.properties')))
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
                        "cluster": "testCluster",
                        "additionalParameters": {
                            "testKey": "testValue"
                        }
                       }
                """
        )
        headers {
            header(location(), "http://localhost/cnwlcm/v1/operations/test-operation-id")
        }
    }
}

