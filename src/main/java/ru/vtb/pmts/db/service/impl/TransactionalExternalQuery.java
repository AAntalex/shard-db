package ru.vtb.pmts.db.service.impl;

import ru.vtb.pmts.db.model.enums.QueryType;
import ru.vtb.pmts.db.service.abstractive.AbstractTransactionalQuery;
import ru.vtb.pmts.db.service.api.ResultQuery;

import java.sql.*;
import java.util.*;

public class TransactionalExternalQuery extends AbstractTransactionalQuery {
    private final List<List<String>> binds = new ArrayList<>();
    private final List<Class<?>> types = new ArrayList<>();
    private List<String> currentBinds = new ArrayList<>();
    private int currentRow;


    TransactionalExternalQuery(String query, QueryType queryType) {
        this.query = query;
        this.queryType = queryType;
    }

    @Override
    public void init() {
        super.init();
        this.currentRow = 0;
        this.types.clear();
        this.currentBinds.clear();
        this.binds.clear();
        this.binds.add(this.currentRow, this.currentBinds);
    }

    @Override
    public void bindOriginal(int idx, Object o) throws Exception {
        if (this.currentRow == 0) {
            this.types.add(idx - 1, o.getClass());
        }
        this.currentBinds.add(idx - 1, o.toString());
    }

    @Override
    public void addBatchOriginal() throws Exception {
        this.binds.add(this.currentRow, this.currentBinds);
        this.currentRow++;
        this.currentBinds = new ArrayList<>();
    }

    @Override
    public int[] executeBatch() throws Exception {
        return null;
    }

    @Override
    public int executeUpdate() throws Exception {
        return 0;
    }

    @Override
    public ResultQuery executeQuery() throws Exception {
        return null;
    }
}
