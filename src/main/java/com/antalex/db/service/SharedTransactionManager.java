package com.antalex.db.service;

import javax.persistence.EntityTransaction;
import java.util.UUID;

public interface SharedTransactionManager {
    EntityTransaction getTransaction();
    EntityTransaction getCurrentTransaction();
    void setAutonomousTransaction();
    void setParallelRun(Boolean parallelRun);
    UUID getTransactionUUID();
}
