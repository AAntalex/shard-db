package ru.vtb.pmts.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.UUID;

@Data
@Accessors(chain = true, fluent = true)
public class RemoteTaskContainer {
    private Shard shard;
    private UUID taskUuid;
    private UUID clientUuid;
    private boolean postponedCommit;
}
