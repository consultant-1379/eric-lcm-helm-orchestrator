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

package com.ericsson.oss.management.lcm.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.HelmfileExecutorApplication;
import com.ericsson.oss.management.lcm.model.entity.Release;
import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;

@ActiveProfiles("test")
@SpringBootTest(classes = HelmfileExecutorApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ReleaseParserTest extends AbstractDbSetupTest {

    @Autowired
    private ReleaseParser parser;

    private static final String SOURCE = "["
            + "{\"name\":\"cn-am-test-app-a\",\"namespace\":\"demo-sprint-9\",\"enabled\":true,\"labels\":\"\","
            + "\"chart\":\"test-charts/cn-am-test-app-a\",\"version\":\"\"},"
            + "{\"name\":\"cn-am-test-app-c\","
            + "\"namespace\":\"demo-sprint-9\",\"enabled\":false,\"labels\":\"\",\"chart\":\"./test-charts/cn-am-test-app-c\",\"version\":\"\"}]\n";

    @Test
    void shouldParseSuccessfully() {
        List<Release> result = parser.parse(SOURCE);
        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(2);
        Release releaseA = result.get(0);
        assertThat(releaseA.getName()).isEqualTo("cn-am-test-app-a");
        assertThat(releaseA.isEnabled()).isTrue();
        Release releaseC = result.get(1);
        assertThat(releaseC.getName()).isEqualTo("cn-am-test-app-c");
        assertThat(releaseC.isEnabled()).isFalse();
    }

    @Test
    void shouldFailForInvalidSource() {
        String source = SOURCE.replace("\"", "");
        assertThatThrownBy(() -> parser.parse(source))
                .isInstanceOf(InternalRuntimeException.class);
    }

}