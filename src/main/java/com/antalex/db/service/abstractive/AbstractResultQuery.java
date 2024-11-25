package com.antalex.db.service.abstractive;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import com.antalex.db.service.api.ResultQuery;

import javax.sql.rowset.serial.SerialClob;
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

@Slf4j
public abstract class AbstractResultQuery implements ResultQuery {
    private static final ZoneOffset DEFAULT_TIME_ZONE = OffsetDateTime.now().getOffset();

    @Override
    public Clob getClob(int idx) throws Exception {
        return new SerialClob(
                Optional.ofNullable(getString(idx))
                        .orElse(StringUtils.EMPTY)
                        .toCharArray()
        );
    }

    @Override
    public URL getURL(int idx) throws Exception {
        String url = getString(idx);
        return url == null ? null : new URL(url);
    }

    @Override
    public <T> T getObject(int idx, Class<T> clazz) throws Exception {
        if (clazz.isEnum()) {
            return (T) Optional.ofNullable(getString(idx))
                    .map(name -> Enum.valueOf((Class<Enum>) clazz, name))
                    .orElse(null);
        }
        if (UUID.class.isAssignableFrom(clazz)) {
            return (T) Optional.ofNullable(getString(idx))
                    .map(str -> UUID.fromString(str))
                    .orElse(null);
        }
        if (Currency.class.isAssignableFrom(clazz)) {
            return (T) Optional.ofNullable(getString(idx))
                    .map(str -> Currency.getInstance(str))
                    .orElse(null);
        }
        return (T) getObject(idx);
    }

    @Override
    public LocalDateTime getLocalDateTime(int idx) throws Exception {
        return Optional.ofNullable(getTimestamp(idx))
                .map(Timestamp::toLocalDateTime)
                .orElse(null);
    }

    @Override
    public OffsetDateTime getOffsetDateTime(int idx) throws Exception {
        return Optional.ofNullable(getLocalDateTime(idx))
                .map(localDateTime -> OffsetDateTime.of(localDateTime, ZoneOffset.UTC))
                .map(offsetDateTimeUTC -> offsetDateTimeUTC.atZoneSameInstant(DEFAULT_TIME_ZONE).toOffsetDateTime())
                .orElse(null);
    }

    @Override
    public LocalDate getLocalDate(int idx) throws Exception {
        return Optional.ofNullable(getDate(idx))
                .map(Date::getTime)
                .map(java.sql.Date::new)
                .map(java.sql.Date::toLocalDate)
                .orElse(null);
    }
}
