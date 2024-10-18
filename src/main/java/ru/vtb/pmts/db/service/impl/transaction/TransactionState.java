package ru.vtb.pmts.db.service.impl.transaction;

import lombok.Data;

@Data
public class TransactionState {
    private boolean completed;
    private boolean hasError;
}
