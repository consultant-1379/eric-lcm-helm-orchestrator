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
package contracts.api.postClusterConnectionInfo.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of adding cluster connection info file

```
given:
  client requests to add cluster connection info file
when:
  a valid request is submitted
then:
  the cluster connection info file is added
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/cluster_connection_info"
        multipart(
                clusterConnectionInfo: named(
                        name: value('clusterConnectionInfo.yaml'),
                        content: $(consumer(nonEmpty()), producer(file('clusterConnectionInfo.yaml')))
                ),
                crdNamespace: value('crdNamespace')
        )
    }
    response {
        status CREATED()
        body(
                """
                       {
                        "id": "testId",
                        "name": "testName",
                        "status": "NOT_IN_USE"
                       }
                """
        )
    }
    priority(1)
}
