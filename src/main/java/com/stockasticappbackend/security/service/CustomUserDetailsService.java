package com.stockasticappbackend.security.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Custom UserDetailsService implementation for Spring Security.
 * Loads user details from the database for authentication.
 * Maps AppUser entities to Spring Security UserDetails.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

        private final AppUserRepository appUserRepository;

        /**
         * Loads a user by their email address for authentication.
         * Creates a Spring Security User with the user's credentials,
         * role, and account status (disabled if not ACTIVE).
         *
         * @param email The user's email address.
         * @return A UserDetails object for authentication.
         * @throws UsernameNotFoundException If no user exists with the email.
         */
        @Override
        public UserDetails loadUserByUsername(String email)
                        throws UsernameNotFoundException {

                AppUser user = appUserRepository.findByEmail(email)
                                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

                return User.builder()
                                .username(user.getEmail())
                                .password(user.getPasswordHash())
                                .roles(user.getRole().name())
                                .disabled(user.getUserStatus() != UserStatus.ACTIVE)
                                .build();
        }
}
