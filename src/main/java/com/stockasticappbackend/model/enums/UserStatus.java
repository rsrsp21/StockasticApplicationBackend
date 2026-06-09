package com.stockasticappbackend.model.enums;

/**
 * Enumeration representing user account statuses.
 */
public enum UserStatus {

	/** Account is active and can log in. */
	ACTIVE,

	/** Account has been blocked by an administrator. */
	BLOCKED,

	/** Account has been soft-deleted by the user. */
	DELETED
}