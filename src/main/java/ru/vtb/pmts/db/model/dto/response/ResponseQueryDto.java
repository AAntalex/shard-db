package ru.vtb.pmts.db.model.dto.response;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class ResponseQueryDto {
    private UUID clientUuid;
    private List<List<String>> result;
}
