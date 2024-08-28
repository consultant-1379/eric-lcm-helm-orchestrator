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

package contracts.api.getAllWorkloadInstanceVersions.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario for wrong formed request when getting all workload instance versions with pagination

```
given:
  client requests workload instance versions with an invalid page number
when:
  a request with is submitted
then:
  fails with 400 response code

```

""")
    request {
        method GET()
        urlPath("/cnwlcm/v1/workload_instances/testId/versions") {
            queryParameters {
                parameter 'page': "-1"
                parameter 'size': value(consumer(regex(nonEmpty()).asInteger()), producer(2))
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
        body(
                """
                       {
                          "type":"Bad Request",
                          "title":"InvalidPaginationQueryException",
                          "status":400,
                          "detail": "Invalid page number:: -1, page number must be greater than 0",
                          "instance":""
                       }
                """
        )
    }
}