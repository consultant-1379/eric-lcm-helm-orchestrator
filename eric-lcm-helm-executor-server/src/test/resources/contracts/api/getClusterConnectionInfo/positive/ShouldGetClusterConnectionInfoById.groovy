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
package contracts.api.getClusterConnectionInfo.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario for getting the cluster connection info DTO by id.

```
given:
  client requests to view the cluster connection info by id
when:
  a valid request is made
then:
  cluster connection info is displayed.
```

""")
    request {
        method 'GET'
        url "/cnwlcm/v1/cluster_connection_info/existing_id"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body(
                """
                       {
                        "id": "existing_id",
                        "name": "testName",
                        "status": "NOT_IN_USE"
                       }
                """

        )
    }
}
