package com.antalex.db.service.api;

import java.math.BigDecimal;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

public interface ResultQuery {
    boolean next() throws Exception;
    Object getObject(int idx) throws Exception;
    <T> T getObject(int idx, Class<T> clazz) throws Exception;
    Long getLong(int idx) throws Exception;
    Short getShort(int idx) throws Exception;
    Boolean getBoolean(int idx) throws Exception;
    String getString(int idx) throws Exception;
    Byte getByte(int idx) throws Exception;
    Integer getInteger(int idx) throws Exception;
    Float getFloat(int idx) throws Exception;
    Double getDouble(int idx) throws Exception;
    BigDecimal getBigDecimal(int idx) throws Exception;
    Date getDate(int idx) throws Exception;
    Time getTime(int idx) throws Exception;
    Timestamp getTimestamp(int idx) throws Exception;
    Blob getBlob(int idx) throws Exception;
    Clob getClob(int idx) throws Exception;
    URL getURL(int idx) throws Exception;
    RowId getRowId(int idx) throws Exception;
    SQLXML getSQLXML(int idx) throws Exception;
    LocalDateTime getLocalDateTime(int idx) throws Exception;
    LocalDate getLocalDate(int idx) throws Exception;
}
