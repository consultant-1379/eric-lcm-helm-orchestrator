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

package contracts.api.putRollbackWorkload.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of rollback a Workload Instance

```
given:
  client requests to rollback a Workload Instance
when:
  a request is submitted
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/workload_instances/valid_id/operations"
        multipart(
                workloadInstanceOperationPutRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('invalid-multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status BAD_REQUEST()
    }
    priority(1)
}
