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

package contracts.api.getAllWorkloadInstances.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario for wrong formed request when getting all workload instances with pagination

```
given:
  client requests workload instances with an invalid page number
when:
  a request with is submitted
then:
  fails with 400 response code

```

""")
    request {
        method GET()
        urlPath("/cnwlcm/v1/workload_instances") {
            queryParameters {
                parameter 'page': value(consumer(regex("^([^1-9]*)\$")), producer("page"))
                parameter 'size': value(consumer(regex(nonEmpty()).asInteger()), producer(2))
            }
        }
        headers {
            accept(applicationJson())
        }
    }
    response {
        status BAD_REQUEST()
    }
}