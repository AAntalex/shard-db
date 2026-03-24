package com.antalex.db.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DynamicDataBaseInfo {
    private String segment;
    private Boolean accessible;
    private Boolean available;
    private Long lastTime;
}
