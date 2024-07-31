package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.dto.query.QueryDto;

import java.util.UUID;

public interface RemoteDatabaseService {
    String executeQuery(QueryDto query);
    String executeUpdate(QueryDto query);
    String executeBatch(QueryDto query);
    void commit(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception;
    void rollback(UUID clientUuid, UUID taskUuid, Boolean postponedCommit) throws Exception;
}
