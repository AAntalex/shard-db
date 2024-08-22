package ru.vtb.pmts.db.service.impl;

import oracle.jdbc.OracleConnection;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.service.LockProcessor;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Component
public class OracleLockProcessor implements LockProcessor<OracleConnection> {
    private static final String QUERY =
            "select bs.SID, bs.SERIAL#, bs.PROGRAM, bs.MACHINE, bs.OSUSER, bs.USERNAME, bs.STATUS, s.SQL_ID\n" +
                    "from gv$session s\n" +
                    "  join gv$session bs on bs.SID = s.FINAL_BLOCKING_SESSION \n" +
                    "                        and bs.INST_ID = s.FINAL_BLOCKING_INSTANCE and bs.STATUS = 'INACTIVE'\n" +
                    "where s.SID = ? and s.SERIAL# = ?";

    @Override
    public String getLockInfo(OracleConnection conn, DataBaseInstance instance) {
        try (Connection connection = instance.getDataSource().getConnection()) {
            Field field = conn.getClass().getDeclaredField("sessionId");
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY);
            Field sessionIdField = conn.getClass().getDeclaredField("sessionId");
            Field serialNumberField  = conn.getClass().getDeclaredField("serialNumber");
            sessionIdField.setAccessible(true);
            serialNumberField.setAccessible(true);
            int sessionId = (int) sessionIdField.get(conn);
            int serialNumber = (int) serialNumberField.get(conn);
            preparedStatement.setInt(1, sessionId);
            preparedStatement.setInt(2, serialNumber);
            ResultSet result = preparedStatement.executeQuery();
            if (result.next()) {
                return "blocked session - SID = " + sessionId +
                        ", SERIAL# = " + serialNumber +
                        ", SQL_ID = " + result.getString(8) +
                        "; blocking session - SID = " + result.getInt(1) +
                        ", SERIAL# = " + result.getInt(2) +
                        ", PROGRAM = " + result.getString(3) +
                        ", MACHINE = " + result.getString(4) +
                        ", OSUSER = " + result.getString(5) +
                        ", USERNAME = " + result.getString(6) +
                        ", STATUS = " + result.getString(7);
            }
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
        return null;
    }
}