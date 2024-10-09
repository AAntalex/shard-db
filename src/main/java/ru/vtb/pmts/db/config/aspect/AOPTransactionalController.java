package ru.vtb.pmts.db.config.aspect;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.service.ShardEntityManager;


@Aspect
@Component
@RequiredArgsConstructor
public class AOPTransactionalController {
    private final ShardEntityManager entityManager;

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void callTransactional() { }

    @Around("callTransactional()")
    public Object aroundProfiler(ProceedingJoinPoint joinPoint) throws Throwable {
            try {
                entityManager.getTransaction().begin();
                return joinPoint.proceed();
            } catch (Exception err) {
                entityManager.getTransaction().rollback();
                throw new RuntimeException(err);
            } finally {
                entityManager.getTransaction().commit();
            }
    }
}
