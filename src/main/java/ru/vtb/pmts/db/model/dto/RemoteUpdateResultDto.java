package ru.vtb.pmts.db.model.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class RemoteUpdateResultDto {
    private UUID clientUuid;
    private int result;
}
