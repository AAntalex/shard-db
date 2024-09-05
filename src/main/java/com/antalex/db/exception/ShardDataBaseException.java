package com.antalex.db.exception;

import java.io.Serial;

public class ShardDataBaseException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -2083713320278095296L;

    public ShardDataBaseException() {
    }

    public ShardDataBaseException(String var1) {
        super(var1);
    }

    public ShardDataBaseException(String var1, Throwable var2) {
        super(var1, var2);
    }

    public ShardDataBaseException(Throwable var1) {
        super(var1);
    }

    protected ShardDataBaseException(String var1, Throwable var2, boolean var3, boolean var4) {
        super(var1, var2, var3, var4);
    }
}
