package ru.vtb.pmts.db.service.impl.sequences;

import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.service.abstractive.AbstractSequenceGenerator;
import ru.vtb.pmts.db.utils.ShardUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class ApplicationSequenceGenerator extends AbstractSequenceGenerator {
    private static final String QUERY_LOCK = """
            SELECT LAST_VALUE,MIN_VALUE,CACHE_SIZE,MAX_VALUE,CYCLE_FLAG
              FROM $$$.APP_SEQUENCE
            WHERE SEQUENCE_NAME = ? FOR UPDATE""";

    private static final String QUERY_UPDATE = "UPDATE $$$.APP_SEQUENCE SET LAST_VALUE = ? WHERE SEQUENCE_NAME = ?";

    private final String name;
    private final DataBaseInstance shard;
    private Integer cacheSize;
    private Connection connection;

    public ApplicationSequenceGenerator(String name, DataBaseInstance shard) {
        this.name = name;
        this.shard = shard;
        this.cacheSize = shard.getSequenceCacheSize();
    }

    public void setCacheSize(Integer cacheSize) {
        this.cacheSize = cacheSize;
    }

    private Connection getConnection() throws SQLException {
        if (Objects.isNull(this.connection) || this.connection.isClosed()) {
            this.connection = shard.getDataSource().getConnection();
            this.connection.setAutoCommit(false);
        }
        return this.connection;
    }

    private void closeConnection() {
        try {
            if (Objects.nonNull(this.connection) && !this.connection.isClosed()) {
                this.connection.close();
            }
        } catch (Exception err) {
            throw new ShardDataBaseException(err, this.shard);
        }
    }

    @Override
    public void init() {
        try {
            Connection connection = getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(
                    ShardUtils.transformSQL(QUERY_LOCK, this.shard)
            );
            preparedStatement.setString(1, this.name);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                this.value = resultSet.getLong(1) + 1L;
                if (resultSet.wasNull()) {
                    this.value = resultSet.getLong(2);
                }
                this.maxValue = Optional.ofNullable(cacheSize)
                        .map(it -> this.value + it)
                        .orElse(this.value + Long.max(resultSet.getLong(3), 1L)) - 1L;

                preparedStatement = connection.prepareStatement(
                        ShardUtils.transformSQL(QUERY_UPDATE, this.shard)
                );

                Long sequenceMaxValue = resultSet.getLong(4);
                if (!resultSet.wasNull() && this.maxValue.compareTo(sequenceMaxValue) > 0) {
                    this.maxValue = sequenceMaxValue;
                    if (resultSet.getBoolean(5)) {
                        preparedStatement.setLong(1, resultSet.getLong(2) - 1L);
                    } else {
                        if (this.value.compareTo(this.maxValue) >= 0) {
                            connection.rollback();
                            throw new ShardDataBaseException(
                                    String.format(
                                            "Достигли придельного значения счетчика \"%s\" - %d",
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
                    throw new ShardDataBaseException(String.format("Ошбка инициализации счетчика \"%s\"", this.name));
                }
            }
            connection.commit();
        } catch (Exception err) {
            throw new ShardDataBaseException(err, this.shard);
        } finally {
            closeConnection();
        }
    }
}
