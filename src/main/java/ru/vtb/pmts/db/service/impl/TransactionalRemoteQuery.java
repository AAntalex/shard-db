package ru.vtb.pmts.db.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.RemoteTaskContainer;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.RemoteButchResultDto;
import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalQuery;
import ru.vtb.pmts.db.service.api.ResultQuery;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TransactionalRemoteQuery extends AbstractTransactionalQuery {
    private final List<List<String>> binds = new ArrayList<>();
    private final List<Class<?>> types = new ArrayList<>();
    private final RemoteTaskContainer taskContainer;
    private final ObjectMapper objectMapper;
    private List<String> currentBinds = new ArrayList<>();
    private int currentRow;

    TransactionalRemoteQuery(
            String query,
            QueryType queryType,
            RemoteTaskContainer taskContainer,
            ObjectMapper objectMapper) {
        this.query = query;
        this.queryType = queryType;
        this.taskContainer = taskContainer;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void bindOriginal(int idx, Object o) throws SQLException {
        if (Objects.isNull(o)) {
            bindOriginal(idx, null, null);
            return;
        }
        if (o instanceof Clob) {
            bindOriginal(idx, ((Clob) o).getSubString(1, (int) ((Clob) o).length()), String.class);
            return;
        }
        if (o instanceof Blob) {
            bindOriginal(idx,  new String(((Blob) o).getBytes(1L, (int) ((Blob) o).length())), Blob.class);
            return;
        }
        if (o instanceof URL) {
            bindOriginal(idx, ((URL) o).toExternalForm(), String.class);
            return;
        }
        if (o instanceof Enum) {
            bindOriginal(idx, ((Enum<?>) o).name(), String.class);
            return;
        }
        if (o instanceof Currency) {
            bindOriginal(idx, ((Currency) o).getCurrencyCode(), String.class);
            return;
        }
        bindOriginal(idx, o.toString(), o.getClass());
    }

    @Override
    protected void bindOriginal(int idx, String o, Class<?> clazz) {
        if (this.currentRow > 0 && Objects.isNull(this.types.get(idx - 1))) {
            this.types.set(idx - 1, clazz);
        } else if (this.currentRow == 0) {
            this.types.add(idx - 1, clazz);
        }
        this.currentBinds.add(idx - 1, o);
    }

    @Override
    public void addBatchOriginal() throws Exception {
        this.currentRow++;
        this.binds.add(this.currentBinds);
        this.currentBinds = new ArrayList<>();
    }

    @Override
    public int[] executeBatch() throws Exception {
        String body = objectMapper.writeValueAsString(getQueryDto());
        initBinds();
        return Optional.ofNullable(
                        taskContainer
                                .shard()
                                .getWebClient()
                                .post()
                                .uri("/api/v1/db/request/executeBatch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError,
                                        response -> response.bodyToMono(String.class).map(ShardDataBaseException::new))
                                .bodyToMono(RemoteButchResultDto.class)
                                .block()
                )
                .map(RemoteButchResultDto::result)
                .orElseThrow(() ->
                        new ShardDataBaseException(
                                "Не смогли получить ответ от " +
                                        taskContainer.shard().getName() +
                                        " при выполнении удаленного запроса"
                        )
                );
    }

    @Override
    public int executeUpdate() throws Exception {
        initBinds();
        return 0;
    }

    @Override
    public ResultQuery executeQuery() throws Exception {
        initBinds();
        return null;
    }

    private void initBinds() {
        this.types.clear();
        this.currentBinds.clear();
        this.binds.clear();
        this.binds.add(this.currentBinds);
        this.currentRow = 0;
    }

    private QueryDto getQueryDto() {
        return new QueryDto()
                .shardId(taskContainer.shard().getId())
                .clusterName(taskContainer.shard().getClusterName())
                .postponedCommit(taskContainer.postponedCommit())
                .taskUuid(taskContainer.taskUuid())
                .clientUuid(taskContainer.clientUuid())
                .query(query)
                .queryType(queryType)
                .types(
                        types
                                .stream()
                                .map(clazz ->
                                        Optional.ofNullable(clazz)
                                                .map(Class::getCanonicalName)
                                                .orElse(null)
                                )
                                .collect(Collectors.toList())
                )
                .currentBinds(currentBinds)
                .binds(binds);
    }
}
