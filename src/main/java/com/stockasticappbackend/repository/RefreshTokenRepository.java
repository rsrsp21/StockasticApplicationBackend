package com.stockasticappbackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    @Modifying
    int deleteByUser(AppUser user);
    
    // Delete a specific token by its string value (uses derived query)
    @Modifying
    @Query("DELETE FROM refreshtoken r WHERE r.token = :token")
    void deleteByToken(@Param("token") String token);
    
    // Count active tokens for a user
    @Query("SELECT COUNT(r) FROM refreshtoken r WHERE r.user.userId = :userId")
    int countByUserId(@Param("userId") Long userId);
    
    // Find oldest tokens for a user (to delete when limit exceeded)
    @Query("SELECT r FROM refreshtoken r WHERE r.user.userId = :userId ORDER BY r.expiryDate ASC")
    List<RefreshToken> findByUserIdOrderByExpiryDateAsc(@Param("userId") Long userId);
}
