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

package contracts.api.getValues.positive

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description("""
Represents a successful scenario of Get values

```
given:
  client requests with existing valuesId
when:
  a valid request is submitted
then:
  the response is returned
```

""")
    request {
        method 'GET'
            url "/cnwlcm/v1/values/values_id"
    }
    response {
        status OK()
        body(
                """global:
                                  crd:
                                    enabled: true
                                    namespace: eric-crd-ns
                                  pullSecret: regcred-successfulpost
                                  registry:
                                    url: armdocker.rnd.ericsson.se
                                  app:
                                    namespace: dima
                                    enabled: true
                                  chart:
                                    registry: ''
                                cn-am-test-app-a:
                                  enabled: true
                                  fuu: bar
                                  name: cn-am-test-app-a
                                cn-am-test-app-b:
                                  enabled: true
                                  fuu: bar
                                  name: cn-am-test-app-b
                                cn-am-test-app-c:
                                  enabled: false
                                  fuu: bar
                                  name: cn-am-test-app-c
                                cn-am-test-crd:
                                  enabled: false
                                  fuu: bar"""
        )
    }
}

