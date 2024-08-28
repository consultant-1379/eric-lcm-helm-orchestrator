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

package contracts.api.getAllClusterConnectionInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario for wrong formed request when getting all cluster connection info with pagination

```
given:
  client requests cluster connection info with an invalid page number
when:
  a request with is submitted
then:
  fails with 400 response code

```

""")
    request {
        method GET()
        urlPath("/cnwlcm/v1/cluster_connection_info") {
            queryParameters {
                parameter 'page': value(consumer(regex(nonEmpty()).asInteger()), producer(2))
                parameter 'size': value(consumer(regex("^([^1-9]*)\$")), producer("size"))
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