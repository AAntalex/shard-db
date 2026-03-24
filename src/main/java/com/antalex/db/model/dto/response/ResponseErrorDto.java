package com.antalex.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseErrorDto {
    private UUID clientUuid;
    private String error;
}
