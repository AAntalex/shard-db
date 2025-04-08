package com.antalex.db.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true, fluent = true)
public class CriteriaElement {
    private String tableName;
    private String tableAlias;
    private List<String> columns;
}
