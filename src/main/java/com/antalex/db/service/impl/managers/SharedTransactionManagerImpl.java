package com.antalex.db.service.impl.managers;

import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.impl.SharedEntityTransaction;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import java.util.Optional;
import java.util.UUID;

@Component
public class SharedTransactionManagerImpl implements SharedTransactionManager {
    private final ThreadLocal<SharedEntityTransaction> transaction = new ThreadLocal<>();
    private Boolean parallelRun;

    @Override
    public EntityTransaction getTransaction() {
        return Optional.ofNullable(this.transaction.get())
                .filter(it -> !it.isCompleted())
                .orElseGet(() -> {
                    this.transaction.set(
                            Optional.ofNullable(this.transaction.get())
                                    .map(SharedEntityTransaction::getParentTransaction)
                                    .orElse(new SharedEntityTransaction(this.parallelRun))
                    );
                    return this.transaction.get();
                });
    }

    @Override
    public EntityTransaction getCurrentTransaction() {
        return this.transaction.get();
    }

    @Override
    public void setAutonomousTransaction() {
        SharedEntityTransaction transaction = new SharedEntityTransaction(this.parallelRun);
        transaction.setParentTransaction(this.transaction.get());
        this.transaction.set(transaction);
    }

    @Override
    public void setParallelRun(Boolean parallelRun) {
        this.parallelRun = parallelRun;
    }

    @Override
    public UUID getTransactionUUID() {
        return Optional.ofNullable(this.transaction.get())
                .map(SharedEntityTransaction::getUuid)
                .orElse(null);
    }
}
