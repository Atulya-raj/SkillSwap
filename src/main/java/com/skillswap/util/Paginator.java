package com.skillswap.util;

import java.util.Collections;
import java.util.List;

/**
 * Pagination helper and immutable page container.
 */
public final class Paginator {

    private Paginator() {
    }

    /**
     * Calculates the SQL query offset for the given 1-indexed page.
     *
     * @param page 1-indexed page number
     * @param pageSize page size
     * @return 0-indexed SQL offset
     */
    public static int calculateOffset(int page, int pageSize) {
        int safePage = Math.max(1, page);
        return (safePage - 1) * pageSize;
    }

    /**
     * Calculates total pages for a given total count and page size.
     *
     * @param totalItems total count of items
     * @param pageSize items per page
     * @return total page count
     */
    public static int calculateTotalPages(int totalItems, int pageSize) {
        if (pageSize <= 0) return 1;
        return (int) Math.ceil((double) totalItems / pageSize);
    }

    /**
     * Immutable container for a paginated slice of items.
     *
     * @param <T> item type
     */
    public static class Page<T> {
        private final List<T> items;
        private final int page;
        private final int pageSize;
        private final int totalItems;
        private final int totalPages;

        /**
         * Constructs a Page instance.
         *
         * @param items list of items on this page
         * @param page current 1-indexed page number
         * @param pageSize items per page
         * @param totalItems total number of items across all pages
         */
        public Page(List<T> items, int page, int pageSize, int totalItems) {
            this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
            this.page = Math.max(1, page);
            this.pageSize = Math.max(1, pageSize);
            this.totalItems = totalItems;
            this.totalPages = calculateTotalPages(totalItems, this.pageSize);
        }

        public List<T> getItems() {
            return items;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public int getTotalItems() {
            return totalItems;
        }

        public int getTotalPages() {
            return Math.max(1, totalPages);
        }

        public boolean hasPrevious() {
            return page > 1;
        }

        public boolean hasNext() {
            return page < totalPages;
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
}
