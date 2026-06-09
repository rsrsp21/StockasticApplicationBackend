package com.stockasticappbackend.model.entity;

import java.time.LocalDateTime;

import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.stockasticappbackend.model.enums.UserRole;
import com.stockasticappbackend.model.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;

/**
 * Entity representing a user in the application.
 * Stores user account information including credentials, profile data,
 * and account status.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "app_user", uniqueConstraints = {
		@UniqueConstraint(columnNames = "email"),
		@UniqueConstraint(columnNames = "mobile")
})
public class AppUser {

	/** The unique identifier of the user. */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long userId;

	/** The user's display name. */
	@Column(nullable = false)
	private String name;

	/** The user's email address (unique). */
	@Column(nullable = false)
	private String email;

	/** The BCrypt hashed password. */
	@Column(nullable = false)
	private String passwordHash;

	/** The user's mobile number (optional, unique). */
	private String mobile;

	/** The path to the user's profile image file. */
	private String profileImagePath;

	/** The current account status. */
	@Enumerated(EnumType.STRING)
	private UserStatus userStatus = UserStatus.ACTIVE;

	/** The user's role for authorization. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role = UserRole.USER;

	/** Timestamp when the user was created. */
	@CreationTimestamp
	private LocalDateTime createdAt;

	/** Timestamp when the user was last updated. */
	@UpdateTimestamp
	private LocalDateTime updatedAt;

	/** The user's wallet (bidirectional). */
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Wallet wallet;

	/** The user's KYC record (bidirectional). */
	@OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private Kyc kyc;

	/** user relations **/
	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<BankAccount> bankAccounts;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Holdings> holdings;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Notification> notifications;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Order> orders;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<PriceAlert> priceAlerts;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<RefreshToken> refreshTokens;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Sip> sips;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<AutoSellRule> autoSellRules;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<Watchlist> watchlists;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@ToString.Exclude
	@EqualsAndHashCode.Exclude
	private List<ActivityLog> activityLogs;
}