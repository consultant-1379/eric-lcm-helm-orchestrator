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
Represents an error scenario of Deletion a Workload instance, when instance is not terminated

```
given:
  client requests to delete a non terminated instance
when:
  a request with a workloadInstanceId is submitted
then:
  the request is rejected with 409 CONFLICT
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/workload_instances/not_terminated"
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type":"Conflict",
                          "title":"Conflict Exception",
                          "status":409,
                          "detail":"Workload instance must be TERMINATED before deletion. As an exclusion instance that doesn't have any successful operation can be deleted as well",
                          "instance":""
                       }
                """
        )
    }
    priority(3)
}
