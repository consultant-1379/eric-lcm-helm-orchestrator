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

package contracts.api.putUpdateValues.negative

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a failure scenario of values updating.

```
given:
  client requests to update values with incorrect version
when:
  values with given version not found
then:
  the request is rejected with 404 NOT FOUND
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/values/not_existent_version"
        multipart(
                valuesRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-data.json'))),
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
                          "detail": "Values with version ${fromRequest().path(3)} not found",
                          "instance":""
                       }
                """
        )
    }
}

