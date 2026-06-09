package com.stockasticappbackend.model.enums;

/**
 * Enumeration representing KYC verification statuses.
 */
public enum KycStatus {

	/** KYC submission is awaiting review. */
	PENDING,

	/** KYC has been verified and approved. */
	APPROVED,

	/** KYC has been rejected. */
	REJECTED,

	/** User has not submitted KYC yet. */
	NOT_ATTEMPTED
}
