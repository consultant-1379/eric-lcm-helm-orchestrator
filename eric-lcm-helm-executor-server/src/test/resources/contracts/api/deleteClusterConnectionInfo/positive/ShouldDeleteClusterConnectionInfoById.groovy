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

package contracts.api.deleteClusterConnectionInfo.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of deletion a cluster connection info

```
given:
  cluster connection info id
when:
  deletion called with appropriate cluster connection info id
then:
  cluster connection info should be deleted with response NO CONTENT.
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/cluster_connection_info/existing_id"
    }
    response {
        status NO_CONTENT()
    }
    priority 1
}

