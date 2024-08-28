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

package contracts.api.getWorkloadInstance.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Get Workload Instance info

```
given:
  client requests with existing workloadInstanceId
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'GET'
        url "/cnwlcm/v1/workload_instances/workloadInstanceId"
    }
    response {
        status OK()
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
    }
}