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
package contracts.api.terminateWorkload.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of termination a Workload

```
given:
  client requests to terminate a Workload
when:
  a valid request is submitted
then:
  the Workload is terminated
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/workload_instances/dummy_id/operations"
        multipart(
                workloadInstanceOperationPostRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-json-part.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status ACCEPTED()
        headers {
            header(location(), "http://localhost/cnwlcm/v1/operations/test-operation-id")
        }
    }
    priority(1)
}

