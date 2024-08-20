package ru.vtb.pmts.db.service.impl.managers;

import com.zaxxer.hikari.pool.ProxyConnection;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.GenericTypeResolver;
import org.springframework.stereotype.Component;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.DataBaseInstance;
import ru.vtb.pmts.db.service.LockManager;
import ru.vtb.pmts.db.service.LockProcessor;
import ru.vtb.pmts.db.service.ShardEntityRepository;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.util.*;

@Component
@Primary
public class LockManagerImpl implements LockManager {
    private static final Map<Class<?>, LockProcessor> LOCK_PROCESSORS = new HashMap<>();

    private final ThreadLocal<LockProcessor<?>> currentLockProcessor = new ThreadLocal<>();
    private final ThreadLocal<Connection> currentConnection = new ThreadLocal<>();
    private final Field delegateField;

    LockManagerImpl() {
        try {
            this.delegateField = ProxyConnection.class.getDeclaredField("delegate");
            this.delegateField.setAccessible(true);
        } catch (Exception err) {
            throw new ShardDataBaseException(err);
        }
    }

    @Autowired
    public void setLockProcessors(List<LockProcessor<?>> lockProcessors) {
        for (LockProcessor<?> lockProcessor : lockProcessors) {
            Class<?>[] classes = GenericTypeResolver
                    .resolveTypeArguments(lockProcessors.getClass(), ShardEntityRepository.class);
            if (Objects.nonNull(classes) && classes.length > 0 && !LOCK_PROCESSORS.containsKey(classes[0])) {
                LOCK_PROCESSORS.put(classes[0], lockProcessor);
            }
        }
    }

    @Override
    public <T extends Connection> String getLockInfo(T conn, DataBaseInstance shard) {
        if (conn == null) {
            return null;
        }
        return getLockProcessor(conn).getLockInfo(conn, shard);
    }

    private Class<?> getConnectionClass(Connection connection) {
        if (connection instanceof ProxyConnection) {
            try {
                return this.delegateField.get(connection).getClass();
            } catch (Exception err) {
                throw new ShardDataBaseException(err);
            }
        } else {
            return connection.getClass();
        }
    }

    private <T extends Connection> LockProcessor<T> getLockProcessor(Connection connection) {
        LockProcessor lockProcessor = currentLockProcessor.get();
        if (lockProcessor != null && currentConnection.get() == connection) {
            return lockProcessor;
        }
        Class<?> clazz = getConnectionClass(connection);
        lockProcessor =
                Optional
                        .ofNullable(LOCK_PROCESSORS.get(clazz))
                        .orElse(
                                LOCK_PROCESSORS
                                        .keySet()
                                        .stream()
                                        .filter(it -> it.isAssignableFrom(clazz))
                                        .map(LOCK_PROCESSORS::get)
                                        .findAny()
                                        .orElseThrow(NotImplementedException::new)
                        );
        currentLockProcessor.set(lockProcessor);
        currentConnection.set(connection);
        return lockProcessor;
    }
}
