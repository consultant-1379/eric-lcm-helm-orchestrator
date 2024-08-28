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
package contracts.api.deleteClusterConnectionInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario during deletion a cluster connection info

```
given:
  cluster connection info with id not_found_id not registered
when:
  deletion called with not_found_id cluster connection info id
then:
   the request is rejected
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/cluster_connection_info/not_found_id"
        headers {
            accept(applicationJson())
        }
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type": "Not Found",
                          "title": "Not Found Exception",
                          "status": 404,
                          "detail": "ClusterConnectionInfo with id not_found_id not found",
                          "instance": ""
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
}
