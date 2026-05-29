package com.antalex.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseClusterInfoDto {
    private short clusterId;
    private boolean defaultCluster;
    private List<ResponseInstanceInfoDto> instances;
}
