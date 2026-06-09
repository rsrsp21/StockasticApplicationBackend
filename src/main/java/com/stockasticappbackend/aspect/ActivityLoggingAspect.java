package com.stockasticappbackend.aspect;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.stockasticappbackend.dto.user.UserResponse;
import com.stockasticappbackend.model.entity.AppUser;
import com.stockasticappbackend.model.entity.ActivityLog;
import com.stockasticappbackend.repository.ActivityLogRepository;
import com.stockasticappbackend.repository.AppUserRepository;
import com.stockasticappbackend.service.activity.ActivityLogInternalService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Aspect for logging relevant business activities to the database.
 **/
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class ActivityLoggingAspect {

    private final ActivityLogInternalService internalService;
    private final AppUserRepository userRepository;

    @Pointcut("execution(* com.stockasticappbackend.service..*.create*(..)) || " +
            "execution(* com.stockasticappbackend.service..*.update*(..)) || " +
            "execution(* com.stockasticappbackend.service..*.delete*(..)) || " +
            "execution(* com.stockasticappbackend.service..*.add*(..)) || " +
            "execution(* com.stockasticappbackend.service..*.remove*(..))")
    public void modifyMethods() {
    }

    @AfterReturning(pointcut = "modifyMethods()", returning = "result")
    public void logSuccess(JoinPoint joinPoint, Object result) {
        saveLog(joinPoint, "SUCCESS", null, result);
    }

    @AfterThrowing(pointcut = "modifyMethods()", throwing = "ex")
    public void logFailure(JoinPoint joinPoint, Throwable ex) {
        saveLog(joinPoint, "FAILURE", ex.getMessage(), null);
    }

    private void saveLog(JoinPoint joinPoint, String status, String errorDetails, Object result) {
        try {
            String methodName = joinPoint.getSignature().getName();
            String className = joinPoint.getTarget().getClass().getSimpleName();
            Object[] args = joinPoint.getArgs();

            AppUser user = null;
            // 1. Try to resolve from arguments
            for (Object arg : args) {
                if (arg instanceof Long) {
                    user = userRepository.findById((Long) arg).orElse(null);
                    if (user != null) break;
                } else if (arg instanceof String && ((String) arg).contains("@")) {
                    user = userRepository.findByEmail((String) arg).orElse(null);
                    if (user != null) break;
                } else if (arg instanceof AppUser) {
                    user = (AppUser) arg;
                    break;
                }
            }

            // 2. Try to resolve from result (e.g., for createUser where arg is CreateUserRequest)
            if (user == null && result != null) {
                if (result instanceof UserResponse) {
                    user = userRepository.findById(((UserResponse) result).getUserId()).orElse(null);
                } else if (result instanceof AppUser) {
                    user = (AppUser) result;
                }
            }

            // 3. Try to resolve from authenticated principal (HTTP request flow)
            if (user == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null
                        && authentication.isAuthenticated()
                        && !"anonymousUser".equals(authentication.getName())) {
                    user = userRepository.findByEmail(authentication.getName()).orElse(null);
                }
            }

            if (user == null) {
                // Background jobs/schedulers run without authenticated user; skip quietly.
                log.debug("Skipping activity log without resolved user: {}.{}", className, methodName);
                return;
            }

            String description = String.format("%s.%s called", className, methodName);
            String details = "Args: " + Arrays.toString(args);
            if (errorDetails != null) {
                details += " | Error: " + errorDetails;
            }

            ActivityLog logEntry = ActivityLog.builder()
                    .activityType(methodName.toUpperCase())
                    .description(description)
                    .user(user)
                    .timestamp(LocalDateTime.now())
                    .status(status)
                    .details(details.length() > 500 ? details.substring(0, 500) : details)
                    .build();

            internalService.save(logEntry);

        } catch (Exception e) {
            log.error("Failed to save activity log", e);
        }
    }
}
