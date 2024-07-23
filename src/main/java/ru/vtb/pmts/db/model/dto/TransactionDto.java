package ru.vtb.pmts.db.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class TransactionDto {
    private String clusterName;
    private Short shardId;
    private UUID transactionUuid;
    private UUID clientUuid;
    private boolean needCommit;
}
