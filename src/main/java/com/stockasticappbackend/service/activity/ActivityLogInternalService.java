package com.stockasticappbackend.service.activity;

import com.stockasticappbackend.model.entity.ActivityLog;
import com.stockasticappbackend.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.scheduling.annotation.Async;

@Service
@RequiredArgsConstructor
public class ActivityLogInternalService {

    private final ActivityLogRepository activityLogRepository;

    /**
     * Saves the activity log in a NEW transaction.
     * This ensures the log is written even if the calling method's transaction rolls back.
     */
    @Async("activityLogExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(ActivityLog logEntry) {
        if (logEntry == null || logEntry.getUser() == null) {
            return;
        }
        activityLogRepository.save(logEntry);
    }
}
