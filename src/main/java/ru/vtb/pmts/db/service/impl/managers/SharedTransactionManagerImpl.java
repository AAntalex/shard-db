package ru.vtb.pmts.db.service.impl.managers;

import ru.vtb.pmts.db.model.TransactionInfo;
import ru.vtb.pmts.db.service.SharedTransactionManager;
import ru.vtb.pmts.db.service.impl.transaction.SharedEntityTransaction;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SharedTransactionManagerImpl implements SharedTransactionManager {
    private final ThreadLocal<SharedEntityTransaction> transaction = new ThreadLocal<>();
    private final List<TransactionInfo> transactionInfoList = new ArrayList<>();
    private Boolean parallelRun;

    @Override
    public EntityTransaction getTransaction() {
        return Optional.ofNullable(this.transaction.get())
                .filter(it -> !it.isCompleted())
                .orElseGet(() -> {
                    this.transaction.set(
                            Optional.ofNullable(this.transaction.get())
                                    .map(SharedEntityTransaction::getParentTransaction)
                                    .orElse(new SharedEntityTransaction(parallelRun, transactionInfoList))
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
        SharedEntityTransaction transaction = new SharedEntityTransaction(parallelRun, transactionInfoList);
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

    @Override
    public List<TransactionInfo> getTransactionInfoList() {
        return transactionInfoList;
    }
}
