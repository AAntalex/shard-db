package ru.vtb.pmts.db.model.dto;

import lombok.Builder;
import lombok.Data;
import ru.vtb.pmts.db.model.enums.QueryType;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class QueryDto {
    private String Query;
    private QueryType queryType;
    private final List<List<String>> binds = new ArrayList<>();
    private final List<String> types = new ArrayList<>();
}
