package com.stockasticappbackend.dto.stock;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for bulk stock upload operations.
 * Reports the total number of rows processed, how many succeeded/failed,
 * and detailed error information for each failed row.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUploadResponse {

    /** Total number of data rows processed from the file. */
    private int totalRows;

    /** Number of rows successfully imported. */
    private int successCount;

    /** Number of rows that failed to import. */
    private int failureCount;

    /** Number of rows skipped because a stock with the same symbol already exists. */
    private int skippedCount;

    /** Detailed error information for each failed row. */
    private List<RowError> errors;

    /**
     * Represents an error that occurred while processing a specific row.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RowError {

        /** The row number in the uploaded file (1-indexed, excluding header). */
        private int row;

        /** The stock symbol from the failed row (if available). */
        private String symbol;

        /** The error message describing what went wrong. */
        private String message;
    }
}
