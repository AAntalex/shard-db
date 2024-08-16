package ru.vtb.pmts.db.service.impl;

import oracle.jdbc.OracleConnection;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.service.LockProcessor;

@Component
public class OracleLockProcessor implements LockProcessor<OracleConnection> {

    @Override
    public String getLockInfo(OracleConnection conn) {
        return "";
    }
}