package com.antalex.db.service.impl;

import com.antalex.db.service.abstractive.AbstractSequenceGenerator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class ApplicationSequenceGenerator extends AbstractSequenceGenerator {
    private static final String QUERY_LOCK = "SELECT LAST_VALUE,MIN_VALUE,CACHE_SIZE,MAX_VALUE,CYCLE_FLAG\n" +
            "  FROM APP_SEQUENCE\n" +
            "WHERE SEQUENCE_NAME = ? FOR UPDATE";

    private static final String QUERY_UPDATE = "UPDATE APP_SEQUENCE SET LAST_VALUE = ? WHERE SEQUENCE_NAME = ?";

    private String name;
    private DataSource dataSource;
    private Integer cacheSize;
    private Connection connection;

    public ApplicationSequenceGenerator(String name, DataSource dataSource) {
        this.name = name;
        this.dataSource = dataSource;
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    private Connection getConnection() throws SQLException {
        if (Objects.isNull(this.connection) || this.connection.isClosed()) {
            this.connection = dataSource.getConnection();
            this.connection.setAutoCommit(false);
        }
        return this.connection;
    }

    @Override
    public void init() {
        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(QUERY_LOCK);
            preparedStatement.setString(1, this.name);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                this.value = resultSet.getLong(1);
                if (resultSet.wasNull()) {
                    this.value = resultSet.getLong(2);
                }
                this.maxValue = Optional.ofNullable(cacheSize)
                        .map(it -> this.value + it)
                        .orElse(this.value + Long.max(resultSet.getLong(3), 1L));

                preparedStatement = connection.prepareStatement(QUERY_UPDATE);

                Long sequenceMaxValue = resultSet.getLong(4);
                if (!resultSet.wasNull() && this.maxValue.compareTo(sequenceMaxValue) > 0) {
                    this.maxValue = sequenceMaxValue;
                    if (resultSet.getBoolean(5)) {
                        preparedStatement.setLong(1, resultSet.getLong(2));
                    } else {
                        if (this.value.compareTo(this.maxValue) >= 0) {
                            connection.rollback();
                            throw new RuntimeException(
                                    String.format(
                                            "Достигли придельнго значения счетчика \"%s\" - %d",
                                            this.name,
                                            this.maxValue
                                    )
                            );
                        }
                    }
                } else {
                    preparedStatement.setLong(1, this.maxValue);
                }
                preparedStatement.setString(2, this.name);
                if (preparedStatement.executeUpdate() == 0) {
                    connection.rollback();
                    throw new RuntimeException(String.format("Ошбка инициализации счетчика \"%s\"", this.name));
                }
            }
            connection.commit();
        } catch (Exception err) {
            throw new RuntimeException(err);
        }
    }
}
