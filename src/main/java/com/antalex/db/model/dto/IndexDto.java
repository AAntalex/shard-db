package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class IndexDto {
    private String name;
    private Boolean unique;
    private String columnList;
}
