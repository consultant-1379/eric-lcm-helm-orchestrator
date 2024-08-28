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
package contracts.api.getClusterConnectionInfo.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an unsuccessful scenario for getting the cluster connection info by id.

```
given:
   client requests to view a not existing cluster connection info
when:
   a request with an id of cluster connection info is submitted
then:
   the request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'GET'
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
                          "type":"Not Found",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"ClusterConnectionInfo with id not_found_id not found",
                          "instance":""
                       }
                """

        )
    }
    priority(1)
}
