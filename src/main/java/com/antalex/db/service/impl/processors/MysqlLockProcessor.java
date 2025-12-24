package com.antalex.db.service.impl.processors;

import com.antalex.db.exception.ShardDataBaseException;
import com.antalex.db.model.DataBaseInstance;
import com.antalex.db.service.LockProcessor;
import com.mysql.cj.jdbc.JdbcConnection;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

@Component
public class MysqlLockProcessor implements LockProcessor<JdbcConnection> {
    private static final String QUERY = """
            select bs.pid, bs.application_name, bs.client_addr, bs.usename, bs.state, s.query
            from pg_stat_activity s
              join pg_stat_activity bs on bs.backend_xid = s.backend_xmin and bs.state like 'idle%'
            where s.pid = ?""";

    @Override
    public String getLockInfo(JdbcConnection targetConnection, DataBaseInstance instance) {
        try (Connection connection = instance.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);

        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
        return null;
    }
}