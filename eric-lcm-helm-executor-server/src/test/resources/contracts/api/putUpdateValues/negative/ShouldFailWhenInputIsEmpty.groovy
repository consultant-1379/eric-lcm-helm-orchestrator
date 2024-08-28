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
  client requests to update values
when:
  values for update are empty
then:
  the request is rejected with 400 BAD REQUEST
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/values/existing_version"
        multipart(
                valuesRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('empty-multipart-data.json'))),
                        contentType: value("application/json")
                ),
                values: named(
                        name: value('values.yaml'),
                        content: $(consumer(nonBlank()), producer(file('empty-values.yaml.properties'))),
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
                          "detail": "Values and additional parameters can't be empty",
                          "instance":""
                       }
                """
        )
    }
}

