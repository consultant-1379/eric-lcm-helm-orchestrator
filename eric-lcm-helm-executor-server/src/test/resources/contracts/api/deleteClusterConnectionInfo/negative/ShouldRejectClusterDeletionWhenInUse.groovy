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
  cluster connection info with id in_use_id are still used
when:
  deletion called
then:
  the request is rejected with response CONFLICT
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/cluster_connection_info/in_use_id"
    }
    response {
        status CONFLICT()
        body(
                """
                       {
                          "type": "Conflict",
                          "title": "Cluster IN_USE exception",
                          "status": 409,
                          "detail": "Cluster with id in_use_id still IN_USE, you can't delete it.",
                          "instance": ""
                       }
                """
        )
        headers {
            contentType(applicationJson())
        }
    }
    priority 1
}
