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

package contracts.api.postHelmfileFetcherWorkloadInstance.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents an error scenario of Updating a Workload Instance via Helmfile Fetcher

```
given:
  client requests to updating a Workload Instance via Helmfile Fetcher instance without 'workloadInstanceWithURLPutRequestDto'
when:
  an invalid request is submitted
then:
  the request is rejected with 404 Bad Request
```

""")
    request {
        method 'PUT'
        urlPath("/cnwlcm/v1/helmfile_fetcher/workload_instances/testId") {
            queryParameters {
                parameter 'isUrlToHelmRegistry': true
            }
        }
        multipart(
                workloadInstanceWithURLPutRequestDto: named(
                        name: value('request.json'),
                        content: value(''),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status BAD_REQUEST()
        body(
                """
                {
                "type":"Bad Request",
                "title":"Malformed Request",
                "status":400,
                "detail":"Required part 'workloadInstanceWithURLPutRequestDto' is not present.",
                "instance":""
                }
                """
        )
    }
}

