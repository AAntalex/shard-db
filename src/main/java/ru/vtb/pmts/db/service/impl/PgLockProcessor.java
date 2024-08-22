package ru.vtb.pmts.db.service.impl;

import org.postgresql.jdbc.PgConnection;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.service.LockProcessor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class PgLockProcessor implements LockProcessor<PgConnection> {
    private static final String QUERY = "select bs.pid, bs.application_name, bs.client_addr, bs.usename, bs.state, s.query\n" +
            "from pg_stat_activity s\n" +
            "  join pg_stat_activity bs on bs.backend_xid = s.backend_xmin and bs.state like 'idle%'\n" +
            "where s.pid = ?";

    @Override
    public String getLockInfo(PgConnection targetConnection, DataBaseInstance instance) {
        try (Connection connection = instance.getDataSource().getConnection()) {
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            preparedStatement.setInt(1, targetConnection.getBackendPID());
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                return "blocked session - pid = " + targetConnection.getBackendPID() +
                        ", query = " + result.getString(6) +
                        "; blocking session - pid = " + result.getInt(1) +
                        ", application_name = " + result.getString(2) +
                        ", client_addr = " + result.getString(3) +
                        ", usename = " + result.getString(4) +
                        ", state = " + result.getString(5);
            }
        } catch (SQLException err) {
            throw new ShardDataBaseException(err);
        }
        return null;
    }
}