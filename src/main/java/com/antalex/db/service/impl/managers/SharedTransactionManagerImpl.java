package com.antalex.db.service.impl.managers;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.TransactionInfo;
import com.antalex.db.service.SharedTransactionManager;
import com.antalex.db.service.impl.transaction.SharedEntityTransaction;
import org.springframework.stereotype.Component;

import javax.persistence.EntityTransaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

@Component
public class SharedTransactionManagerImpl implements SharedTransactionManager {
    private final ThreadLocal<SharedEntityTransaction> transaction = new ThreadLocal<>();
    private final List<TransactionInfo> transactionInfoList = new ArrayList<>();
    private Boolean parallelRun;

    @Override
    public EntityTransaction getTransaction() {
        return Optional.ofNullable(this.transaction.get())
                .filter(it -> !it.getState().isCompleted())
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

    @Override
    public void runInTransaction(Runnable runnable) {
        EntityTransaction entityTransaction = getTransaction();
        boolean isAurTransaction = !entityTransaction.isActive();
        if (isAurTransaction) {
            entityTransaction.begin();
        }
        try {
            runnable.run();
            if (isAurTransaction) {
                checkTransaction((SharedEntityTransaction) entityTransaction);
                entityTransaction.commit();
            }
        } catch (Exception err) {
            if (isAurTransaction) {
                checkTransaction((SharedEntityTransaction) entityTransaction);
                entityTransaction.rollback();
            }
            throw new ShardDataBaseException(err);
        }
    }

    @Override
    public <T> T runInTransaction(Callable<T> callable) {
        EntityTransaction entityTransaction = getTransaction();
        boolean isAurTransaction = !entityTransaction.isActive();
        if (isAurTransaction) {
            entityTransaction.begin();
        }
        try {
           return callable.call();
        } catch (Exception err) {
            if (isAurTransaction) {
                checkTransaction((SharedEntityTransaction) entityTransaction);
                entityTransaction.rollback();
            }
            throw new ShardDataBaseException(err);
        } finally {
            if (isAurTransaction) {
                checkTransaction((SharedEntityTransaction) entityTransaction);
                entityTransaction.commit();
            }
        }
    }

    private void checkTransaction(SharedEntityTransaction transaction) {
        EntityTransaction currentTransaction = getCurrentTransaction();
        if (currentTransaction != transaction && (transaction.isActive() || currentTransaction.isActive())) {
            throw new ShardDataBaseException(
                    "Нарушена целостность при обработке транзакции с UUID = " + transaction.getUuid()
            );
        }
    }
}
