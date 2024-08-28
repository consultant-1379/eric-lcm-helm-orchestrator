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

package contracts.api.getWorkloadInstanceVersion.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Get Workload Instance Version info

```
given:
  client requests with existing workloadInstanceId and version
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'GET'
        url "/cnwlcm/v1/workload_instances/workloadInstanceId/versions/1"
    }
    response {
        status OK()
        body(
                """
                       {
                        "id": "firstId",
                        "version": "1",
                        "helmSourceVersion": "1.2.3-4",
                        "valuesVersion": "0e35ed30-d438-4b07-a82b-cab447424d30"
                       }
                """
        )
    }
}