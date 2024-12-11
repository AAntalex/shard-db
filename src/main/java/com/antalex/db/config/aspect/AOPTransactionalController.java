package com.antalex.db.config.aspect;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.service.SharedTransactionManager;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;


@Aspect
@Component
@RequiredArgsConstructor
public class AOPTransactionalController {
    private final SharedTransactionManager transactionManager;

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void callTransactional() { }


    @Around("callTransactional()")
    public Object aroundTransactional(ProceedingJoinPoint joinPoint) {
        return transactionManager.runInTransaction(() -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable err) {
                throw new ShardDataBaseException(err);
            }
        });
    }
}
