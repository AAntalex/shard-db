package ru.vtb.pmts.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseUpdateDto {
    private UUID clientUuid;
    private int result;
}
