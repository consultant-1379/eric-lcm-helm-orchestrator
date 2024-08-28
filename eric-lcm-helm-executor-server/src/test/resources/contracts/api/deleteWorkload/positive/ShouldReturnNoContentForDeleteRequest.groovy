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
package contracts.api.deleteWorkload.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of deletion a Workload

```
given:
  client requests to delete a Workload
when:
  a valid request is submitted
then:
  the Workload is deleted
```

""")
    request {
        method 'DELETE'
        url "/cnwlcm/v1/workload_instances/dummy_id"
    }
    response {
        status NO_CONTENT()
    }
    priority(1)
}

