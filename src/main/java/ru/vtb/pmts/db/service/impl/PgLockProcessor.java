package ru.vtb.pmts.db.service.impl;

import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.service.LockProcessor;

@Component
public class PgLockProcessor implements LockProcessor<PgConnection> {

    @Override
    public String getLockInfo(PgConnection conn) {
        return "";
    }
}