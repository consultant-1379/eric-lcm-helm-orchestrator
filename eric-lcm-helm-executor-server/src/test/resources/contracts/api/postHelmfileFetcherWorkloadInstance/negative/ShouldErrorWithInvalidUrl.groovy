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
  client requests to instantiate a Workload instance with invalid url
when:
  an invalid request is submitted
then:
  the request is rejected with 404 Not Found
```

""")
    request {
        method 'POST'
        url "/cnwlcm/v1/helmfile_fetcher/workload_instances"
        multipart(
                workloadInstanceWithURLRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('invalid-url-multipart-data.json'))),
                        contentType: value("application/json")
                )
        )
    }
    response {
        status NOT_FOUND()
        body(
                """
                       {
                          "type":"Not Found",
                          "title":"Not Found Exception",
                          "status":404,
                          "detail":"Helmfile or integration chart by url = https://localhost:8080/invalid-url was NOT FETCHED. Something went wrong, details: null",
                          "instance":""
                       }
                """
        )
    }
}

