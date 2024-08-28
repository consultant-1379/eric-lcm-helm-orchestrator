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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.ericsson.oss.management.lcm.AbstractDbSetupTest;
import com.ericsson.oss.management.lcm.model.entity.WorkloadInstance;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidInputException;
import com.ericsson.oss.management.lcm.utils.validator.WorkloadInstanceValidator;

@ActiveProfiles("test")
@SpringBootTest()
class WorkloadInstanceValidatorTest extends AbstractDbSetupTest {

    private static final String LONG_NAMESPACE = "111111111122222222223333333333444444444455555555556666666666-64c";
    private static final String UPPERCASE_NAMESPACE = "UPPERCASE";
    private static final String SYMBOL_NAMESPACE = "%";
    private static final String NAMESPACE_WITH_WRONG_FIRST_SYMBOL = "-name";
    private static final String NAMESPACE_WITH_WRONG_LAST_SYMBOL = "name-";
    private static final String INVALID_NAMESPACE_MESSAGE = "Namespace %s is invalid\n";
    private static final String INVALID_CRD_NAMESPACE_MESSAGE = "CrdNamespace %s is invalid\n";

    @Test
    void shouldReturnNothingWhenValidateValidObject() {
        //Init
        WorkloadInstance workloadInstance = getInstance();

        //Test method
        WorkloadInstanceValidator.validate(workloadInstance);
    }

    @Test
    void shouldThrowExceptionWhenValidateNamespaceTooLong() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setNamespace(LONG_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_NAMESPACE_MESSAGE, LONG_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateCrdNamespaceTooLong() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setCrdNamespace(LONG_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_CRD_NAMESPACE_MESSAGE, LONG_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateNamespaceHaveUppercase() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setNamespace(UPPERCASE_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_NAMESPACE_MESSAGE, UPPERCASE_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateCrdNamespaceHaveUppercase() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setCrdNamespace(UPPERCASE_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_CRD_NAMESPACE_MESSAGE, UPPERCASE_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateNamespaceHaveSpecificSymbols() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setNamespace(SYMBOL_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_NAMESPACE_MESSAGE, SYMBOL_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateCrdNamespaceHaveSpecificSymbols() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setCrdNamespace(SYMBOL_NAMESPACE);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_CRD_NAMESPACE_MESSAGE, SYMBOL_NAMESPACE));
    }

    @Test
    void shouldThrowExceptionWhenValidateNamespaceStartedNonAlphanumeric() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setNamespace(NAMESPACE_WITH_WRONG_FIRST_SYMBOL);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_NAMESPACE_MESSAGE, NAMESPACE_WITH_WRONG_FIRST_SYMBOL));
    }

    @Test
    void shouldThrowExceptionWhenValidateCrdNamespaceStartedNonAlphanumeric() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setCrdNamespace(NAMESPACE_WITH_WRONG_FIRST_SYMBOL);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_CRD_NAMESPACE_MESSAGE, NAMESPACE_WITH_WRONG_FIRST_SYMBOL));
    }

    @Test
    void shouldThrowExceptionWhenValidateNamespaceFinishedNonAlphanumeric() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setNamespace(NAMESPACE_WITH_WRONG_LAST_SYMBOL);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_NAMESPACE_MESSAGE, NAMESPACE_WITH_WRONG_LAST_SYMBOL));
    }

    @Test
    void shouldThrowExceptionWhenValidateCrdNamespaceFinishedNonAlphanumeric() {
        //Init
        WorkloadInstance workloadInstance = getInstance();
        workloadInstance.setCrdNamespace(NAMESPACE_WITH_WRONG_LAST_SYMBOL);

        //Test method and verify
        executeAndAssert(workloadInstance, String.format(INVALID_CRD_NAMESPACE_MESSAGE, NAMESPACE_WITH_WRONG_LAST_SYMBOL));
    }

    private WorkloadInstance getInstance() {

        WorkloadInstance workloadInstance = new WorkloadInstance();
        workloadInstance.setWorkloadInstanceName("name");
        workloadInstance.setNamespace("7namespace-3-test");
        workloadInstance.setCrdNamespace("crdnamespace");
        workloadInstance.setCluster("cluster name");
        workloadInstance.setAdditionalParameters("additionalParameters");

        return workloadInstance;
    }

    private void executeAndAssert(WorkloadInstance workloadInstance, String message) {
        assertThatThrownBy(() -> WorkloadInstanceValidator.validate(workloadInstance))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage(message);
    }
}
