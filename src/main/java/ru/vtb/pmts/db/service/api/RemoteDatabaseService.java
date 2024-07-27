package ru.vtb.pmts.db.service.api;

import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.RemoteButchResultDto;

public interface RemoteDatabaseService {
    RemoteButchResultDto executeBatch(QueryDto query);
}
