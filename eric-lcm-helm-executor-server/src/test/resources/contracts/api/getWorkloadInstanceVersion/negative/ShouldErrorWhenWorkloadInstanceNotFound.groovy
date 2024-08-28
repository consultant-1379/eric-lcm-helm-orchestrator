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

package contracts.api.getWorkloadInstanceVersion.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Getting an Workload Instance Version

```
given:
  client requests to get an Workload Instance Version info with incorrect workloadInstanceId
when:
  an operation not found
then:
  the request is rejected with 404 Not Found
```

""")
    request {
        method 'GET'
        url "/cnwlcm/v1/workload_instances/not_found/versions/1"
    }
    response {
        status NOT_FOUND()
        body(
                """
                      {
                          "type":"Not Found",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail": "Version ${fromRequest().path(5)} by workloadInstanceId = ${fromRequest().path(3)} was not found",
                          "instance":""
                       }
                """
        )
    }
}