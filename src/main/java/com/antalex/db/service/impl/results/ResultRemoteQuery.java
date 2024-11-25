package com.antalex.db.service.impl.results;

import com.antalex.db.service.abstractive.AbstractResultQuery;
import com.antalex.db.exception.ShardDataBaseException;

import javax.sql.rowset.serial.SerialBlob;
import java.math.BigDecimal;
import java.sql.*;
import java.sql.Date;
import java.util.*;

public class ResultRemoteQuery extends AbstractResultQuery {
    private final List<List<String>> result;
    private List<String> currentResult;
    private final Integer fetchLimit;
    private int currentIndex;

    public ResultRemoteQuery(List<List<String>> result, Integer fetchLimit) {
        this.result = result;
        this.fetchLimit = fetchLimit;
    }

    @Override
    public int getColumnCount() {
        return result.isEmpty() ? 0 : result.get(0).size();
    }

    @Override
    public boolean next() {
        if ((fetchLimit == null || currentIndex+1 <= fetchLimit) && result.size() > currentIndex) {
            currentResult = result.get(currentIndex++);
            return true;
        }
        return false;
    }

    @Override
    public Long getLong(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Long::parseLong).orElse(null);
    }

    @Override
    public Object getObject(int idx) throws Exception {
        return getString(idx);
    }

    @Override
    public Boolean getBoolean(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Boolean::parseBoolean).orElse(null);
    }

    @Override
    public Short getShort(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Short::parseShort).orElse(null);
    }

    @Override
    public String getString(int idx) throws Exception {
        return currentResult.get(idx-1);
    }

    @Override
    public BigDecimal getBigDecimal(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(BigDecimal::new).orElse(null);
    }

    @Override
    public Byte getByte(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Byte::parseByte).orElse(null);
    }

    @Override
    public Double getDouble(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Double::parseDouble).orElse(null);
    }

    @Override
    public Float getFloat(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Float::parseFloat).orElse(null);
    }

    @Override
    public Integer getInteger(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Integer::parseInt).orElse(null);
    }

    @Override
    public Date getDate(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(java.sql.Date::valueOf).orElse(null);
    }

    @Override
    public Time getTime(int idx) throws Exception {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Time::valueOf).orElse(null);
    }

    @Override
    public Timestamp getTimestamp(int idx) {
        return Optional.ofNullable(currentResult.get(idx-1)).map(Timestamp::valueOf).orElse(null);
    }

    @Override
    public Blob getBlob(int idx) {
        return Optional
                .ofNullable(currentResult.get(idx-1))
                .map(it -> {
                    try {
                        return new SerialBlob(it.getBytes());
                    } catch (SQLException err) {
                        throw new ShardDataBaseException(err);
                    }
                })
                .orElse(null);
    }

    @Override
    public RowId getRowId(int idx) {
        if (Objects.isNull(currentResult.get(idx-1))) {
            return null;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public SQLXML getSQLXML(int idx) {
        if (Objects.isNull(currentResult.get(idx-1))) {
            return null;
        }
        throw new UnsupportedOperationException();
    }
}
