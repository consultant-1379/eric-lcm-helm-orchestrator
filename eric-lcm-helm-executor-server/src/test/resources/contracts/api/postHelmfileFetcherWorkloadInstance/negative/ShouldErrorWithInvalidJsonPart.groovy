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
Represents an error scenario of Instantiating a Workload Instance via Helmfile Fetcher

```
given:
  client requests to instantiate a Workload instance with invalid Json part via Helmfile Fetcher
when:
  an invalid request is submitted
then:
  the request is rejected with 400 Bad Request
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/helmfile_fetcher/workload_instances"
        multipart(
                workloadInstanceWithURLRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('invalid-multipart-data.json'))),
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
                          "title":"Invalid Input Exception",
                          "status":400,
                          "detail":"WorkloadInstanceName  is invalid\\nNamespace  is invalid\\n",
                          "instance":""
                       }
                """
        )
    }
}

