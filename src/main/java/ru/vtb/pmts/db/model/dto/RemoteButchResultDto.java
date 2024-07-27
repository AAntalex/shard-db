package ru.vtb.pmts.db.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class RemoteButchResultDto {
    private UUID clientUuid;
    private int[] result;
}
