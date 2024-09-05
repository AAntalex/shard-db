package com.antalex.db.service.api;

import com.antalex.db.model.dto.QueryDto;

import java.util.UUID;

public interface RemoteDatabaseService {
    String executeQuery(QueryDto query);
    String executeUpdate(QueryDto query);
    String executeBatch(QueryDto query);
    String getResponseError(String error);
    void commit(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception;
    void rollback(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception;
}
