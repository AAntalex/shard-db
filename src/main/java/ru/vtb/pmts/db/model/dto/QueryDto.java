package ru.vtb.pmts.db.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.vtb.pmts.db.model.enums.QueryType;

import java.util.ArrayList;
import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class QueryDto {
    private TransactionDto transactionInfo;
    private String query;
    private QueryType queryType;
    private final List<List<String>> binds = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
}
