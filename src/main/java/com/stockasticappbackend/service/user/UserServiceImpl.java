package com.stockasticappbackend.service.user;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.stockasticappbackend.dto.user.ChangePasswordRequest;
import com.stockasticappbackend.dto.user.CreateUserRequest;
import com.stockasticappbackend.dto.user.UpdateProfileRequest;
import com.stockasticappbackend.dto.user.UserKycStatusResponse;
import com.stockasticappbackend.dto.user.UserResponse;
import com.stockasticappbackend.exception.EmailAlreadyExistsException;
import com.stockasticappbackend.exception.InvalidCredentialsException;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.mapper.UserMapper;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.Kyc;
import com.stockasticappbackend.model.enums.KycStatus;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.repository.KycRepository;
import com.stockasticappbackend.security.service.RefreshTokenService;
import com.stockasticappbackend.util.Constants;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

	private final AppUserRepository appUserRepository;

	private final PasswordEncoder passwordEncoder;

	private final UserMapper userMapper;

	private final KycRepository kycRepository;

    private final RefreshTokenService refreshTokenService;

	@Value("${file.upload-kyc}")
	private String kycUploadDir;

	@Override
	public void submitKyc(String email, String panNumber, String aadhaarNumber, MultipartFile document) {

		AppUser user = appUserRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));

		if (user.getUserStatus() == UserStatus.BLOCKED) {
			throw new IllegalStateException(Constants.KYC_USER_BLOCKED);
		}

		Kyc kyc = kycRepository.findByUser(user).orElse(null);

		// Re-upload case
		if (kyc != null) {

			if (kyc.getKycStatus() == KycStatus.PENDING) {
				throw new IllegalStateException(Constants.KYC_ALREADY_UNDER_REVIEW);
			}

			if (kyc.getKycStatus() == KycStatus.APPROVED) {
				throw new IllegalStateException(Constants.KYC_ALREADY_APPROVED);
			}

			if (kyc.getAttemptCount() >= 3) {
				user.setUserStatus(UserStatus.BLOCKED);
				appUserRepository.save(user);
                refreshTokenService.deleteByUserId(user.getUserId());
				throw new IllegalStateException(Constants.KYC_MAX_ATTEMPTS);
			}

			kyc.setPanNumber(panNumber);
			kyc.setAadhaarNumber(aadhaarNumber);
			kyc.setDocumentPath(storeDocument(user.getUserId(), document));
			kyc.setKycStatus(KycStatus.PENDING);
			kyc.setAttemptCount(kyc.getAttemptCount() + 1);
			kyc.setRejectionReason(null);
			kyc.setReviewedAt(null);
			kyc.setSubmittedAt(LocalDateTime.now());

			kycRepository.save(kyc);
			return;
		}

		// First-time KYC
		Kyc newKyc = new Kyc();
		newKyc.setUser(user);
		newKyc.setPanNumber(panNumber);
		newKyc.setAadhaarNumber(aadhaarNumber);
		newKyc.setDocumentPath(storeDocument(user.getUserId(), document));
		newKyc.setKycStatus(KycStatus.PENDING);
		newKyc.setSubmittedAt(LocalDateTime.now());

		kycRepository.save(newKyc);
	}

	private String getExtension(String filename) {
		if (filename == null || !filename.contains(".")) {
			return "";
		}
		return filename.substring(filename.lastIndexOf("."));
	}

	private String storeDocument(Long userId, MultipartFile document) {

		if (document == null || document.isEmpty()) {
			throw new IllegalArgumentException(Constants.KYC_DOCUMENT_EMPTY);
		}

		try {
			// user-specific directory
			Path userDir = Paths.get(kycUploadDir, "user_" + userId).toAbsolutePath().normalize();

			Files.createDirectories(userDir);

			// ALWAYS same filename → overwrite
			String extension = getExtension(document.getOriginalFilename());
			String fileName = "kyc" + extension;

			Path filePath = userDir.resolve(fileName);

			// overwrite existing
			document.transferTo(filePath.toFile());

			// save relative path
			return "user_" + userId + "/" + fileName;

		} catch (Exception e) {
			throw new RuntimeException(Constants.KYC_DOCUMENT_STORE_ERROR, e);
		}
	}


	@Value("${file.upload-dir}")
	private String uploadDir;

	@Override
	public UserResponse createUser(CreateUserRequest request) {

		if (appUserRepository.existsByEmail(request.getEmail())) {
			throw new EmailAlreadyExistsException(Constants.EMAIL_ALREADY_EXISTS);
		}

		AppUser user = userMapper.toEntity(request);
		user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

		return userMapper.toResponse(appUserRepository.save(user));
	}

	@Override
	public UserResponse getProfile(String email) {
		return userMapper.toResponse(findUser(email));
	}

	@Override
	public UserResponse updateProfile(String email, UpdateProfileRequest request) {

		AppUser user = findUser(email);

		if (request.getName() != null) {
			user.setName(request.getName());
		}
		if (request.getMobile() != null) {
			user.setMobile(request.getMobile());
		}

		return userMapper.toResponse(appUserRepository.save(user));
	}

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/jpg");

	private String resolveExtension(String contentType) {
		return switch (contentType) {
			case "image/jpeg", "image/jpg" -> ".jpg";
			case "image/png" -> ".png";
			default -> "";
		};
	}

	@Override
	public UserResponse uploadProfileImage(String email, MultipartFile file) {

		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException(Constants.FILE_REQUIRED);
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
			throw new IllegalArgumentException(Constants.PROFILE_IMAGE_TYPE_ERROR);
		}

		AppUser user = findUser(email);

		try {
			Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
			Files.createDirectories(uploadPath);

			if (user.getProfileImagePath() != null) {
				Path oldFilePath = uploadPath.resolve(user.getProfileImagePath()).normalize();
				Files.deleteIfExists(oldFilePath);
			}

			String extension = resolveExtension(contentType);
			String fileName = "user_" + user.getUserId() + extension;
			Path newFilePath = uploadPath.resolve(fileName);

			file.transferTo(newFilePath.toFile());

			user.setProfileImagePath(fileName);
			return userMapper.toResponse(appUserRepository.save(user));

		} catch (Exception e) {
			throw new RuntimeException(Constants.PROFILE_IMAGE_UPLOAD_ERROR, e);
		}
	}

	@Override
	public void deleteProfile(String email) {

		AppUser user = findUser(email);
		user.setUserStatus(UserStatus.DELETED);
		appUserRepository.save(user);
	}

	private AppUser findUser(String email) {
		return appUserRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));
	}

	@Override
	public void changePassword(String email, ChangePasswordRequest request) {

		AppUser user = findUser(email);

		if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {

			throw new InvalidCredentialsException(Constants.INVALID_OLD_PASSWORD);
		}

		user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

		appUserRepository.save(user);
	}

	@Override
	public UserKycStatusResponse getMyKycStatus(String email) {

		AppUser user = appUserRepository.findByEmail(email)
				.orElseThrow(() -> new ResourceNotFoundException(Constants.USER_NOT_FOUND));

		UserKycStatusResponse response = new UserKycStatusResponse();

		Kyc kyc = kycRepository.findByUser(user).orElse(null);

		// No KYC submitted yet
		if (kyc == null) {
			response.setKycStatus(KycStatus.NOT_ATTEMPTED);
			response.setAttemptCount(0);
			return response;
		}

		// KYC exists
		response.setKycStatus(kyc.getKycStatus());
		response.setAttemptCount(kyc.getAttemptCount());
		response.setRejectionReason(kyc.getRejectionReason());
		response.setSubmittedAt(kyc.getSubmittedAt());

		return response;
	}
}
