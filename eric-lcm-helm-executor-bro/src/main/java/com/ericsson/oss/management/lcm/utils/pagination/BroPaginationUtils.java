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

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Comparator;
import com.ericsson.oss.management.lcm.api.model.BackupDto;
import com.ericsson.oss.management.lcm.api.model.BackupManagerDto;
import com.ericsson.oss.management.lcm.api.model.PaginationInfo;
import com.ericsson.oss.management.lcm.api.model.PaginationLinks;
import com.ericsson.oss.management.lcm.api.model.URILink;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidPaginationQueryException;

import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_TYPE;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_STATUS;
import static com.ericsson.oss.management.lcm.constants.BackupAndRestoreConstants.BACKUP_ID;
import static java.util.stream.Collectors.toList;

public final class BroPaginationUtils {
    private BroPaginationUtils(){}
    private static final Pattern REGEX_SPLIT_QUERIES = Pattern.compile("\\s*,\\s*");

    public static Pageable createPageable(Integer pageNumber, Integer pageSize, Pageable defaults) {
        var defaultPageable = Optional.ofNullable(defaults).orElse(CustomPageRequest.of());
        final int page = Optional.ofNullable(pageNumber).orElse(defaultPageable.getPageNumber());
        final int size = Optional.ofNullable(pageSize).orElse(defaultPageable.getPageSize());

        return CustomPageRequest.of(page, size, Sort.unsorted());
    }

    public static List<BackupDto> parseSortBackupDto(List<BackupDto> backups, List<String> sort,
                                                     Set<String> validSortColumns) {
        List<Sort.Order> sortQuery = parseSort(sort, validSortColumns);

        return sortQuery.stream()
                .flatMap(sortOrder -> sortOrder.getDirection().equals(Sort.Direction.DESC) ?
                        descSortingBackups(sortOrder, backups).stream() :
                        acsSortingBackups(sortOrder, backups).stream())
                .collect(Collectors.toList());
    }

    public static List<BackupManagerDto> parseSortBackupManagersDto(List<BackupManagerDto> backupsManagers, List<String> sort,
                                                                    Set<String> validSortColumns) {
        List<Sort.Order> sortQuery = parseSort(sort, validSortColumns);
        return sortQuery.stream()
                .flatMap(sortOrder -> sortOrder.getDirection().equals(Sort.Direction.DESC) ?
                        descSortingBackupManagersDto(sortOrder, backupsManagers).stream() :
                        ascSortingBackupManagersDto(sortOrder, backupsManagers).stream())
                .collect(Collectors.toList());
    }

    private static List<BackupManagerDto> ascSortingBackupManagersDto(Sort.Order query, List<BackupManagerDto> backupsManagers) {
        return backupsManagers.stream()
                .sorted(Comparator.comparing(getBackupManagersComparator(query.getProperty())))
                .collect(toList());
    }

    private static List<BackupManagerDto> descSortingBackupManagersDto(Sort.Order query, List<BackupManagerDto> backupsManagers) {
        return backupsManagers.stream()
                .sorted(Comparator.comparing(getBackupManagersComparator(query.getProperty()))
                        .reversed())
                .collect(toList());
    }

    private static Function<BackupManagerDto, String> getBackupManagersComparator(String property) {
        if (property.equals(BACKUP_ID)) {
            return BackupManagerDto::getId;
        } else if (property.equals(BACKUP_TYPE)) {
            return BackupManagerDto::getBackupType;
        } else {
            return BackupManagerDto::getBackupDomain;
        }
    }

    private static List<BackupDto> descSortingBackups(Sort.Order query, List<BackupDto> backups) {
        return query.getProperty().equals(BACKUP_STATUS) ?
                backups.stream()
                        .sorted(Comparator.comparing(BackupDto::getStatus)
                                .reversed())
                        .collect(toList()) :
                backups.stream()
                        .sorted(Comparator.comparing(BackupDto::getCreationTime)
                                .reversed())
                .collect(toList());
    }

    private static List<BackupDto> acsSortingBackups(Sort.Order query, List<BackupDto> backups) {
        return query.getProperty().equals(BACKUP_STATUS) ?
                backups.stream()
                        .sorted(Comparator.comparing(BackupDto::getStatus))
                        .collect(toList()) :
                backups.stream()
                        .sorted(Comparator.comparing(BackupDto::getCreationTime))
                .collect(toList());
    }

    private static List<Sort.Order> parseSort(List<String> sortQueryList, Set<String> validSortColumns) {
        List<Sort.Order> orders = new ArrayList<>();
        if (sortQueryList.size() == 2) {
            Optional<Sort.Direction> direction = Sort.Direction.fromOptionalString(sortQueryList.get(1));
            direction.ifPresentOrElse(d -> addSingleSortOrder(sortQueryList, validSortColumns, orders),
                () -> addMultipleSortOrders(sortQueryList, validSortColumns, orders));
        } else {
            addMultipleSortOrders(sortQueryList, validSortColumns, orders);
        }
        return orders;
    }

    public static PaginationLinks buildLinks(Page<?> page) {
        final int pageSelf = page.getNumber() + 1;
        final int pageLast = getLastPage(page);
        var builder = ServletUriComponentsBuilder.fromCurrentRequest();
        PaginationLinks links = new PaginationLinks()
                .self(buildLink(pageSelf, builder))
                .first(buildLink(1, builder))
                .last(buildLink(pageLast, builder));
        if (pageSelf > 1) {
            links.prev(buildLink(pageSelf - 1, builder));
        }
        if (pageSelf < page.getTotalPages()) {
            links.next(buildLink(pageSelf + 1, builder));
        }
        return links;
    }

    public static PaginationInfo buildPaginationInfo(Page<?> page) {
        checkMaxPageNumber(page);
        return new PaginationInfo()
                .number(page.getNumber() + 1)
                .size(page.getSize())
                .totalPages(getLastPage(page))
                .totalElements((int) page.getTotalElements());
    }

    private static void addMultipleSortOrders(List<String> sortQueryList, Set<String> validSortColumns, List<Sort.Order> orders) {
        sortQueryList.forEach(query -> {
            List<String> queryItems = getSingleSortQuery(query);
            addSingleSortOrder(queryItems, validSortColumns, orders);
        });
    }

    private static List<String> getSingleSortQuery(String sortQuery) {
        return Arrays.asList(sortQuery.trim().split(REGEX_SPLIT_QUERIES.pattern()));
    }

    private static void addSingleSortOrder(List<String> sortQueryList, Set<String> validSortColumns, List<Sort.Order> orders) {
        var direction = Sort.Direction.ASC;
        if (sortQueryList.size() > 1) {
            direction = getDirection(sortQueryList.get(1));
        }
        checkOrderValues(sortQueryList, validSortColumns);
        var order = new Sort.Order(direction, sortQueryList.get(0)).ignoreCase();
        orders.add(order);
    }

    private static Sort.Direction getDirection(String direction) {
        return Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() ->  new InvalidPaginationQueryException(String
                        .format("Invalid sorting values :: %s. Acceptable values are :: 'desc' or 'asc' (case insensitive)", direction))
                );
    }

    private static void checkOrderValues(final List<String> sortQueryList, final Set<String> validOrderFields) {
        if (!validOrderFields.contains(sortQueryList.get(0))) {
            throw new InvalidPaginationQueryException(String.format("Invalid column value for sorting:: %s. Acceptable values are :: %s",
                    sortQueryList.get(0), validOrderFields));
        }
    }

    private static int getLastPage(final Page<?> page) {
        //totalPages can be 0 if the search result is empty or if the number of items < page size in the search
        return page.getTotalPages() == 0 ? 1 : page.getTotalPages();
    }

    private static URILink buildLink(int page, UriComponentsBuilder builder) {
        builder.replaceQueryParam("page", page);
        return new URILink().href(builder.build().toUriString());
    }

    private static void checkMaxPageNumber(final Page<?> page) {
        int pageNumber = page.getNumber();
        int totalPages = page.getTotalPages();
        if (pageNumber != 0 && pageNumber + 1 > totalPages) {
            throw new InvalidPaginationQueryException(String.format(
                    "Requested page number exceeds the total number of pages. Requested page:: %s. Total "
                            + "page size:: %s",
                    page.getNumber() + 1,
                    page.getTotalPages()));
        }
    }
}
