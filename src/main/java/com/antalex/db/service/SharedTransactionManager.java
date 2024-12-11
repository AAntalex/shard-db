package com.antalex.db.service;

import com.antalex.db.model.TransactionInfo;

import javax.persistence.EntityTransaction;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public interface SharedTransactionManager {
    EntityTransaction getTransaction();
    EntityTransaction getCurrentTransaction();
    void setAutonomousTransaction();
    void setParallelRun(Boolean parallelRun);
    UUID getTransactionUUID();
    List<TransactionInfo> getTransactionInfoList();
    void runInTransaction(Runnable runnable);
    <T> T runInTransaction(Callable<T> callable);
}
