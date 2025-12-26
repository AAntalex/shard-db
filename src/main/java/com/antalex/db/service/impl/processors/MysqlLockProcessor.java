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
    private static final String QUERY = "";

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