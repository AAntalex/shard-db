package com.antalex.db.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.OffsetDateTime;

@Data
@Accessors(chain = true, fluent = true)
public class AttributeHistory {
    private String attributeName;
    private OffsetDateTime time;
    private Object value;
}
