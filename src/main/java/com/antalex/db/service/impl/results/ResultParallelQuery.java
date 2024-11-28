package com.antalex.db.service.impl.results;

import com.antalex.db.service.api.ResultQuery;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.Date;

public class ResultParallelQuery implements ResultQuery {
    private final List<ResultQuery> results = new ArrayList<>();
    private ResultQuery currentResult;
    private int currentIndex;

    public void add(ResultQuery result) {
        if (result instanceof ResultParallelQuery) {
            results.addAll(((ResultParallelQuery) result).results);
        } else {
            this.results.add(result);
        }
    }

    @Override
    public boolean next() throws Exception {
        if (currentIndex >= results.size()) {
            return false;
        }
        if (this.currentResult == null) {
            this.currentResult = results.get(currentIndex);
        }
        if (!currentResult.next()) {
            this.currentIndex++;
            this.currentResult = null;
            return next();
        }
        return true;
    }

    @Override
    public int getColumnCount() throws Exception {
        return currentResult.getColumnCount();
    }

    @Override
    public Long getLong(int idx) throws Exception {
        return currentResult.getLong(idx);
    }

    @Override
    public Object getObject(int idx) throws Exception {
        return currentResult.getObject(idx);
    }

    @Override
    public Short getShort(int idx) throws Exception {
        return currentResult.getShort(idx);
    }

    @Override
    public Boolean getBoolean(int idx) throws Exception {
        return currentResult.getBoolean(idx);
    }

    @Override
    public String getString(int idx) throws Exception {
        return currentResult.getString(idx);
    }

    @Override
    public BigDecimal getBigDecimal(int idx) throws Exception {
        return currentResult.getBigDecimal(idx);
    }

    @Override
    public Blob getBlob(int idx) throws Exception {
        return currentResult.getBlob(idx);
    }

    @Override
    public Byte getByte(int idx) throws Exception {
        return currentResult.getByte(idx);
    }

    @Override
    public Clob getClob(int idx) throws Exception {
        return currentResult.getClob(idx);
    }

    @Override
    public Date getDate(int idx) throws Exception {
        return currentResult.getDate(idx);
    }

    @Override
    public Double getDouble(int idx) throws Exception {
        return currentResult.getDouble(idx);
    }

    @Override
    public Float getFloat(int idx) throws Exception {
        return currentResult.getFloat(idx);
    }

    @Override
    public Integer getInteger(int idx) throws Exception {
        return currentResult.getInteger(idx);
    }

    @Override
    public RowId getRowId(int idx) throws Exception {
        return currentResult.getRowId(idx);
    }

    @Override
    public SQLXML getSQLXML(int idx) throws Exception {
        return currentResult.getSQLXML(idx);
    }

    @Override
    public Time getTime(int idx) throws Exception {
        return currentResult.getTime(idx);
    }

    @Override
    public Timestamp getTimestamp(int idx) throws Exception {
        return currentResult.getTimestamp(idx);
    }

    @Override
    public URL getURL(int idx) throws Exception {
        return currentResult.getURL(idx);
    }

    @Override
    public LocalDateTime getLocalDateTime(int idx) throws Exception {
        return currentResult.getLocalDateTime(idx);
    }

    @Override
    public OffsetDateTime getOffsetDateTime(int idx) throws Exception {
        return currentResult.getOffsetDateTime(idx);
    }

    @Override
    public LocalDate getLocalDate(int idx) throws Exception {
        return currentResult.getLocalDate(idx);
    }

    @Override
    public <T> T getObject(int idx, Class<T> clazz) throws Exception {
        return currentResult.getObject(idx, clazz);
    }
}
