package com.antalex.db.service.impl.results;

import com.antalex.db.service.abstractive.AbstractResultQuery;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Optional;

public class ResultSQLQuery extends AbstractResultQuery {
    private static final ZoneOffset DEFAULT_TIME_ZONE = OffsetDateTime.now().getOffset();

    private final ResultSet result;
    private final Integer fetchLimit;
    private int count;

    public ResultSQLQuery(ResultSet result, Integer fetchLimit) {
        this.result = result;
        this.fetchLimit = fetchLimit;
    }

    @Override
    public int getColumnCount() throws SQLException {
        return result.getMetaData().getColumnCount();
    }

    @Override
    public boolean next() throws SQLException {
        return (fetchLimit == null || ++count <= fetchLimit) && result.next();
    }

    @Override
    public Long getLong(int idx) throws SQLException {
        long ret = result.getLong(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Object getObject(int idx) throws SQLException {
        return result.getObject(idx);
    }

    @Override
    public Boolean getBoolean(int idx) throws SQLException {
        boolean ret = result.getBoolean(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Short getShort(int idx) throws SQLException {
        short ret = result.getShort(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public String getString(int idx) throws SQLException {
        return result.getString(idx);
    }

    @Override
    public BigDecimal getBigDecimal(int idx) throws SQLException {
        BigDecimal ret = result.getBigDecimal(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Byte getByte(int idx) throws SQLException {
        byte ret = result.getByte(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Double getDouble(int idx) throws SQLException {
        double ret = result.getDouble(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Float getFloat(int idx) throws SQLException {
        float ret = result.getFloat(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Integer getInteger(int idx) throws SQLException {
        int ret = result.getInt(idx);
        return result.wasNull() ? null : ret;
    }

    @Override
    public Date getDate(int idx) throws SQLException {
        return result.getDate(idx);
    }

    @Override
    public Time getTime(int idx) throws SQLException {
        return result.getTime(idx);
    }

    @Override
    public Timestamp getTimestamp(int idx) throws SQLException {
        return result.getTimestamp(idx);
    }

    @Override
    public Blob getBlob(int idx) throws SQLException {
        return result.getBlob(idx);
    }

    @Override
    public RowId getRowId(int idx) throws SQLException {
        return result.getRowId(idx);
    }

    @Override
    public SQLXML getSQLXML(int idx) throws SQLException {
        return result.getSQLXML(idx);
    }

    @Override
    public LocalDate getLocalDate(int idx) throws SQLException {
        return Optional.ofNullable(result.getDate(idx))
                .map(java.sql.Date::toLocalDate)
                .orElse(null);
    }
}
