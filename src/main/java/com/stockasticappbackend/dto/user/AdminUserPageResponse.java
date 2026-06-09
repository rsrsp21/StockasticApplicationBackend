package com.stockasticappbackend.dto.user;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for keyset-paginated user listing.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserPageResponse {

    /** The list of users for the current page. */
    private List<AdminUserResponse> users;

    /** The cursor ID for the next page (last user's ID). Null if no more pages. */
    private Long nextCursor;

    /** Whether there are more pages after this one. */
    private boolean hasMore;

    /** Total number of matching users. */
    private long totalElements;

    /** Number of users per page. */
    private int pageSize;
}
