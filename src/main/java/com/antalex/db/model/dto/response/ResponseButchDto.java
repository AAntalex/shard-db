package com.antalex.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseButchDto {
    private UUID clientUuid;
    private int[] result;
}
