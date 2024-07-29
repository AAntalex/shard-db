package ru.vtb.pmts.db.service.impl;

import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalQuery;
import ru.vtb.pmts.db.service.api.ResultQuery;
import ru.vtb.pmts.db.service.impl.results.ResultSQLQuery;

import javax.sql.rowset.serial.SerialBlob;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public class TransactionalSQLQuery extends AbstractTransactionalQuery {
    private final PreparedStatement preparedStatement;
    private static final int FETCH_SIZE = 100000;

    TransactionalSQLQuery(String query, QueryType queryType, PreparedStatement preparedStatement) {
        this.preparedStatement = preparedStatement;
        this.query = query;
        this.queryType = queryType;
    }

    @Override
    protected void bindOriginal(int idx, Object o) throws SQLException {
        if (o instanceof Timestamp) {
            preparedStatement.setTimestamp(idx, (Timestamp) o);
            return;
        }
        if (o instanceof Time) {
            preparedStatement.setTime(idx, (Time) o);
            return;
        }
        if (o instanceof Date) {
            preparedStatement.setDate(idx, new java.sql.Date(((Date) o).getTime()));
            return;
        }
        if (o instanceof Blob) {
            preparedStatement.setBlob(idx, ((Blob) o).getBinaryStream());
            return;
        }
        if (o instanceof Clob) {
            preparedStatement.setString(idx, ((Clob) o).getSubString(1, (int) ((Clob) o).length()));
            return;
        }
        if (o instanceof URL) {
            preparedStatement.setString(idx, ((URL) o).toExternalForm());
            return;
        }
        if (o instanceof LocalDateTime) {
            preparedStatement.setTimestamp(idx, Timestamp.valueOf((LocalDateTime) o));
            return;
        }
        if (o instanceof LocalDate) {
            preparedStatement.setDate(idx, java.sql.Date.valueOf((LocalDate) o));
            return;
        }
        if (o instanceof OffsetDateTime) {
            preparedStatement.setTimestamp(
                    idx,
                    Timestamp.valueOf(((OffsetDateTime) o).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
            );
            return;
        }
        if (o instanceof Enum) {
            preparedStatement.setString(idx, ((Enum<?>) o).name());
            return;
        }
        if (o instanceof UUID) {
            preparedStatement.setString(idx, o.toString());
            return;
        }
        if (o instanceof Currency) {
            preparedStatement.setString(idx, ((Currency) o).getCurrencyCode());
            return;
        }

        preparedStatement.setObject(idx, o);
    }

    @Override
    protected void bindOriginal(int idx, String o, Class<?> clazz) throws SQLException {
        if (o == null) {
            preparedStatement.setObject(idx, null);
            return;
        }
        if (Timestamp.class.isAssignableFrom(clazz)) {
            preparedStatement.setTimestamp(idx, Timestamp.valueOf(o));
            return;
        }
        if (Time.class.isAssignableFrom(clazz)) {
            preparedStatement.setTime(idx, Time.valueOf(o));
            return;
        }
        if (Date.class.isAssignableFrom(clazz)) {
            preparedStatement.setDate(idx, java.sql.Date.valueOf(o));
            return;
        }
        if (Blob.class.isAssignableFrom(clazz)) {
            preparedStatement.setBlob(idx, new SerialBlob(o.getBytes()));
            return;
        }
        if (LocalDateTime.class.isAssignableFrom(clazz)) {
            preparedStatement.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.parse(o)));
            return;
        }
        if (LocalDate.class.isAssignableFrom(clazz)) {
            preparedStatement.setDate(idx, java.sql.Date.valueOf(LocalDate.parse(o)));
            return;
        }
        if (OffsetDateTime.class.isAssignableFrom(clazz)) {
            preparedStatement.setTimestamp(
                    idx,
                    Timestamp.valueOf(OffsetDateTime.parse(o).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
            );
            return;
        }
        preparedStatement.setString(idx, o);
    }

    @Override
    public void addBatchOriginal() throws SQLException {
        this.preparedStatement.addBatch();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        return this.preparedStatement.executeBatch();
    }

    @Override
    public int executeUpdate() throws SQLException {
        return this.preparedStatement.executeUpdate();
    }

    @Override
    public ResultQuery executeQuery() throws SQLException {
        this.preparedStatement.setFetchSize(Optional.ofNullable(this.fetchLimit).orElse(FETCH_SIZE));
        return new ResultSQLQuery(this.preparedStatement.executeQuery(), this.fetchLimit);
    }
}
