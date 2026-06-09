package com.stockasticappbackend.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic paginated response wrapper for API endpoints.
 * Provides metadata for pagination including total elements,
 * total pages, and navigation indicators.
 *
 * @param <T> The type of content being paginated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    /** The content of the current page. */
    private List<T> content;

    /** Current page number (0-indexed). */
    private int page;

    /** Number of elements per page. */
    private int size;

    /** Total number of elements across all pages. */
    private long totalElements;

    /** Total number of pages. */
    private int totalPages;

    /** Whether this is the first page. */
    private boolean first;

    /** Whether this is the last page. */
    private boolean last;

    /** Whether there is a next page available. */
    private boolean hasNext;

    /** Whether there is a previous page available. */
    private boolean hasPrevious;
}
