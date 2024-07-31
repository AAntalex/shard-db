package ru.vtb.pmts.db.model.dto.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class RemoteUpdateResultDto {
    private UUID clientUuid;
    private int result;
}
