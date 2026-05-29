package com.antalex.db.service.api;

import com.antalex.db.model.dto.response.ResponseClusterInfoDto;
import com.antalex.db.model.dto.response.ResponseInstanceInfoDto;

import java.util.Map;

public interface DatabaseInfoService {
    ResponseInstanceInfoDto getDatabaseInfo(String cluster, Short shardId);

    ResponseClusterInfoDto getDatabaseInfo(String cluster);

    Map<String, ResponseClusterInfoDto> getDatabaseInfo();
}
