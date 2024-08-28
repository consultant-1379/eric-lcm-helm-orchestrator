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
package contracts.api.deleteWorkload.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Deletion a Workload

```
given:
  client requests to delete a non existing Workload
when:
  a request with a workloadInstanceId is submitted
then:
  the request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/workload_instances/not_found"
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"Not Found",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"WorkloadInstance with id ${fromRequest().path(3)} not found",
                          "instance":""
                       }
                """
        )
    }
    priority(3)
}

