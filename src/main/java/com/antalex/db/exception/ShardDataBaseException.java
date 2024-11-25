package com.antalex.db.exception;

import com.antalex.db.model.DataBaseInstance;

import java.io.Serial;
import java.sql.SQLTransientConnectionException;
import java.util.Objects;

public class ShardDataBaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2083713320278095296L;
    public static final String NOT_AVAILABLE = "ShardDataBaseException.notAvailable";

    private DataBaseInstance shard;

    public ShardDataBaseException() {
    }

    public ShardDataBaseException(String var1) {
        super(var1);
    }

    public ShardDataBaseException(String var1, DataBaseInstance shard) {
        super(var1.equals(NOT_AVAILABLE) ? "The shard " + shard.getName() + " is not available" : var1);
        this.shard = shard;
    }

    public ShardDataBaseException(String var1, Throwable var2) {
        super(var1, var2);
    }

    public ShardDataBaseException(Throwable var1) {
        super(var1 instanceof ShardDataBaseException && Objects.nonNull(var1.getCause()) ? var1.getCause() : var1);
    }

    public ShardDataBaseException(Throwable var1, DataBaseInstance shard) {
        super(var1);
        this.shard = shard;
        if (var1 instanceof SQLTransientConnectionException) {
            this.shard.getDynamicDataBaseInfo().setAvailable(false);
        }
    }

    protected ShardDataBaseException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4);
    }
}
