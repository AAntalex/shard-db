package ru.vtb.pmts.db.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import ru.vtb.pmts.db.exception.ShardDataBaseException;
import ru.vtb.pmts.db.model.RemoteTaskContainer;
import ru.vtb.pmts.db.model.dto.QueryDto;
import ru.vtb.pmts.db.model.dto.response.ResponseButchDto;
import ru.vtb.pmts.db.model.dto.response.ResponseErrorDto;
import ru.vtb.pmts.db.model.dto.response.ResponseQueryDto;
import ru.vtb.pmts.db.model.dto.response.ResponseUpdateDto;
import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalQuery;
import ru.vtb.pmts.db.service.api.ResultQuery;
import ru.vtb.pmts.db.service.impl.results.ResultRemoteQuery;

import java.net.URL;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;

public class TransactionalRemoteQuery extends AbstractTransactionalQuery {
    private static final String EXECUTE_BATCH_URI = "executeBatch";
    private static final String EXECUTE_UPDATE_URI = "executeUpdate";
    private static final String EXECUTE_QUERY_URI = "executeQuery";

    private final List<List<String>> batchBinds = new ArrayList<>();
    private final List<Class<?>> types = new ArrayList<>();
    private final RemoteTaskContainer taskContainer;
    private final ObjectMapper objectMapper;
    private List<String> binds = new ArrayList<>();

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
        if (o.getClass().isAssignableFrom(Date.class)) {
            bindOriginal(idx, (new java.sql.Date(((Date) o).getTime())).toString(), java.sql.Date.class);
            return;
        }
        bindOriginal(idx, o.toString(), o.getClass());
    }

    @Override
    protected void bindOriginal(int idx, String o, Class<?> clazz) {
        if (!this.batchBinds.isEmpty() && Objects.isNull(this.types.get(idx - 1))) {
            this.types.set(idx - 1, clazz);
        } else if (this.batchBinds.isEmpty()) {
            this.types.add(idx - 1, clazz);
        }
        this.binds.add(idx - 1, o);
    }

    @Override
    public void addBatchOriginal() throws Exception {
        this.batchBinds.add(this.binds);
        this.binds = new ArrayList<>();
    }

    @Override
    public int[] executeBatch() throws Exception {
        ResponseButchDto queryResult = getResponseResult(ResponseButchDto.class, EXECUTE_BATCH_URI);
        taskContainer.clientUuid(queryResult.clientUuid());
        return queryResult.result();
    }

    @Override
    public int executeUpdate() throws Exception {
        ResponseUpdateDto queryResult = getResponseResult(ResponseUpdateDto.class, EXECUTE_UPDATE_URI);
        taskContainer.clientUuid(queryResult.clientUuid());
        return queryResult.result();
    }

    @Override
    public ResultQuery executeQuery() throws Exception {
        ResponseQueryDto queryResult = getResponseResult(ResponseQueryDto.class, EXECUTE_QUERY_URI);
        taskContainer.clientUuid(queryResult.clientUuid());
        return new ResultRemoteQuery(
                queryResult.result(),
                this.fetchLimit
        );
    }

    private <R> R getResponseResult(Class<R> clazz, String uri) throws Exception {
        String body = objectMapper.writeValueAsString(getQueryDto());
        initBinds();
        return Optional.ofNullable(
                        taskContainer
                                .shard()
                                .getWebClient()
                                .post()
                                .uri("/api/v1/db/request/" + uri)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body)
                                .retrieve()
                                .onStatus(HttpStatusCode::isError,
                                        response ->
                                                response.bodyToMono(String.class)
                                                        .map(jsonData -> {
                                                            try {
                                                                ResponseErrorDto errorDto =
                                                                        objectMapper.readValue(
                                                                                jsonData,
                                                                                ResponseErrorDto.class
                                                                        );
                                                                taskContainer.clientUuid(errorDto.clientUuid());
                                                                return errorDto.error();
                                                            } catch (Exception err) {
                                                                return err.getLocalizedMessage();
                                                            }
                                                        })
                                                        .map(ShardDataBaseException::new)
                                )
                                .bodyToMono(String.class)
                                .block()
                )
                .map(jsonData -> {
                    try {
                        return objectMapper.readValue(jsonData, clazz);
                    } catch (Exception err) {
                        throw new ShardDataBaseException(err);
                    }
                })
                .orElseThrow(() ->
                        new ShardDataBaseException(
                                "Не смогли получить ответ от " +
                                        taskContainer.shard().getName() +
                                        " при выполнении удаленного запроса"
                        )
                );
    }

    private void initBinds() {
        this.batchBinds.clear();
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
                                .toList()
                )
                .binds(binds)
                .batchBinds(batchBinds);
    }
}
