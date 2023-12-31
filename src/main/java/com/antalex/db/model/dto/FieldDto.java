package com.antalex.db.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FieldDto {
    private String fieldName;
    private String columnName;
}
