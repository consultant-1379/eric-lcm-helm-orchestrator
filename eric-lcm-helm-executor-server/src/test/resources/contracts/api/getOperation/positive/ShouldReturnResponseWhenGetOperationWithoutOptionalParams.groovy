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

package contracts.api.getOperation.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Get operation details

```
given:
  client requests with existing operationId without optional parameters
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'GET'
            url "/cnwlcm/v1/operations/operation_id"
    }
    response {
        status OK()
        body(
                """
                       {
                            "operationId": "operation_id",
                            "workloadInstanceId": "workloadInstanceId",
                            "state": "FAILED",
                            "type": "INSTANTIATE"
                       }
                """
        )
    }
}

