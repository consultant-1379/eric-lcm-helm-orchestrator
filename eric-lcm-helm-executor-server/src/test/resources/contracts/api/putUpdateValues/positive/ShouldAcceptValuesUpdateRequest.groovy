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

package contracts.api.putUpdateValues.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of updating values

```
given:
  client requests to update values
when:
  a valid request is submitted
then:
  values updated
```

""")
    request {
        method 'PUT'
        url "/cnwlcm/v1/values/existing_version"
        multipart(
                valuesRequestDto: named(
                        name: value('request.json'),
                        content: $(consumer(nonBlank()), producer(file('multipart-data.json'))),
                        contentType: value("application/json")
                ),
                values: named(
                        name: value('values.yaml'),
                        // File extension couldn't be yaml as then it was processed as a contract
                        content: $(consumer(nonEmpty()), producer(file('values.yaml.properties')))
                )
        )
    }
    response {
        status ACCEPTED()
        body(
                """one:
                  one-one: one-one
                two: two
                three: three
                four: four"""
        )
    }
}

