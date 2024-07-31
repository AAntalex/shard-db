package ru.vtb.pmts.db.model.dto.query;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class RemoteQueryResultDto {
    private UUID clientUuid;
    private List<List<String>> result;
}
