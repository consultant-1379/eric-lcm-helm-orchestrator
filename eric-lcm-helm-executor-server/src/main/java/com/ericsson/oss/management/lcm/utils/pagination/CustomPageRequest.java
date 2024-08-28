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

package com.ericsson.oss.management.lcm.utils.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidPaginationQueryException;


public class CustomPageRequest extends PageRequest implements Pageable {

    public static final int DEFAULT_PAGE_SIZE = 15;

    protected CustomPageRequest(final int page, final int size, final Sort sort) {
        super(page, size, sort);
    }

    public static CustomPageRequest of() {
        return new CustomPageRequest(1, DEFAULT_PAGE_SIZE, Sort.unsorted());
    }

    public static CustomPageRequest of(String sort) {
        return new CustomPageRequest(1, DEFAULT_PAGE_SIZE, Sort.by(sort));
    }

    public static CustomPageRequest of(int page, int size, Sort sort) {
        checkQueryParameters(page, size);
        return new CustomPageRequest(page - 1, size, sort);
    }

    private static void checkQueryParameters(final int page, final int size) {
        if (page < 1) {
            throw new InvalidPaginationQueryException(String.format("Invalid page number:: %s, page number must be greater than 0", page));
        }

        if (size < 1) {
            throw new InvalidPaginationQueryException(String.format("Invalid page size:: %s, page size must be greater than 0", size));
        }

        if (size > 100) {
            throw new InvalidPaginationQueryException(String.format("Total size of the results will be shown cannot be more than 100. Requested"
                    + " page size %s", size));
        }
    }
}
