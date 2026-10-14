package com.farmconnect.payment.repository;

import com.farmconnect.payment.model.CallbackLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackLogRepository extends JpaRepository<CallbackLog, Long> {}
