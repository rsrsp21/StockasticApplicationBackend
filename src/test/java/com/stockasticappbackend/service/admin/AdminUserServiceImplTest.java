package com.stockasticappbackend.service.admin;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.stockasticappbackend.dto.user.AdminUserResponse;
import com.stockasticappbackend.exception.ResourceNotFoundException;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.enums.UserStatus;
import com.stockasticappbackend.repository.AppUserRepository;

@SpringBootTest
@Transactional
class AdminUserServiceImplTest {

    @Autowired
    private AdminUserService adminUserService;

    @Autowired
    private AppUserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void getAllUsers_ShouldReturnList() {
        AppUser user = new AppUser();
        user.setEmail("admin_test@example.com");
        user.setName("Admin Test");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        List<AdminUserResponse> result = adminUserService.getAllUsers();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("admin_test@example.com", result.get(0).getEmail());
    }

    @Test
    void blockUser_ShouldChangeStatusToBlocked() {
        AppUser user = new AppUser();
        user.setEmail("block@example.com");
        user.setName("Block User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        adminUserService.blockUser(user.getUserId());
        AppUser updatedUser = userRepository.findById(user.getUserId()).get();
        assertEquals(UserStatus.BLOCKED, updatedUser.getUserStatus());
    }

    @Test
    void unblockUser_ShouldChangeStatusToActive() {
        AppUser user = new AppUser();
        user.setEmail("unblock@example.com");
        user.setName("Unblock User");
        user.setPasswordHash("hash");
        user.setUserStatus(UserStatus.BLOCKED);
        user = userRepository.save(user);
        adminUserService.unblockUser(user.getUserId());
        AppUser updatedUser = userRepository.findById(user.getUserId()).get();
        assertEquals(UserStatus.ACTIVE, updatedUser.getUserStatus());
    }

    @Test
    void blockUser_NotFound_ShouldThrowException() {
        assertThrows(ResourceNotFoundException.class, () -> {
            adminUserService.blockUser(999999L);
        });
    }
}

