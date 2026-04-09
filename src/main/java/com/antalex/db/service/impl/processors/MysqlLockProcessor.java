package com.antalex.db.service.impl.processors;

import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.service.LockProcessor;
import com.mysql.cj.jdbc.JdbcConnection;
import org.springframework.stereotype.Component;

@Component
public class MysqlLockProcessor implements LockProcessor<JdbcConnection> {

    @Override
    public String getLockInfo(JdbcConnection targetConnection, DataBaseInstance instance) {
        return null;
    }
}