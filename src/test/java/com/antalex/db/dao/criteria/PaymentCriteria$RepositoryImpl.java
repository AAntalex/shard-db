package com.antalex.db.dao.criteria;

import com.antalex.db.dao.domain.*;
import com.antalex.db.model.criteria.CriteriaElement;
import com.antalex.db.model.criteria.CriteriaElementJoin;
import com.antalex.db.model.criteria.CriteriaPart;
import com.antalex.db.model.criteria.CriteriaPartJoin;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.DomainEntityManager;
import com.antalex.db.service.ShardDataBaseManager;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Component
public class PaymentCriteria$RepositoryImpl implements CriteriaRepository<PaymentCriteria> {
    private static final List<String> COLUMNS = Arrays.asList(
            "MD.C_NUM",
            "MD.C_SUM",
            "MD.C_DATE",
            "ACC_DT.C_CODE",
            "ACC_CT.C_CODE",
            "CL_DT.C_NAME",
            "CL_CT.C_NAME",
            "EXT_DOC.C_RECEIVER"
    );

    private static final List<String> JOIN_COLUMNS = Arrays.asList(
            "ACC_DT.ID",
            "MD.C_ACC_DT",
            "ACC_CT.ID",
            "MD.C_ACC_CT",
            "EXT_DOC.C_DOC",
            "MD.ID",
            "CL_CT.ID",
            "ACC_CT.C_CLIENT",
            "CL_DT.ID",
            "ACC_DT.C_CLIENT",
            "CL_CAT.ID",
            "CL_CT.C_CATEGORY"
    );


    private final ShardDataBaseManager dataBaseManager;
    private final DomainEntityManager domainManager;
    private CriteriaElement mainElement;

    @Autowired
    PaymentCriteria$RepositoryImpl(DomainEntityManager domainManager, ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
        this.domainManager = domainManager;
        this.mainElement = criteriaElement$md();
    }

    private CriteriaPart toCriteriaPart(CriteriaElement element) {
        CriteriaPart criteriaPart = new CriteriaPart()
                .from(getTableName(element))
                .cluster(element.cluster())
                .columns(element.columns());

        for (CriteriaElementJoin join: element.joins()) {
            if (criteriaPart.cluster() == join.element().cluster() &&
                    (
                            dataBaseManager.getEnabledShards(criteriaPart.cluster()).count() == 1 ||
                                    join.linked()



                    )
            ) {
                joinElement(criteriaPart, join);
            } else {
                criteriaPart
                        .joins()
                        .add(new CriteriaPartJoin()
                                .criteriaPart(toCriteriaPart(join.element()))
                                .joinColumns(join.joinColumns())
                                .joinType(join.joinType())
                        );
            }
        }
        return criteriaPart;
    }

    private String getTableName(CriteriaElement element) {
        return element.tableName() + " " + element.tableAlias();
    }

    private String getOn(Pair<Integer, Integer> joinColumns) {
        return " ON " + JOIN_COLUMNS.get(joinColumns.getLeft()) + "=" + JOIN_COLUMNS.get(joinColumns.getRight());
    }

    private void joinElement(CriteriaPart criteriaPart, CriteriaElementJoin join) {
        criteriaPart
                .from(
                        criteriaPart.from() +
                                getJoinText(join.joinType()) +
                                getTableName(join.element()) +
                                getOn(join.joinColumns())
                )
                .columns(criteriaPart.columns() | join.element().columns());
    }

    private String getJoinText(JoinType joinType) {
        return switch (joinType) {
            case INNER -> " JOIN ";
            case LEFT -> " LEFT JOIN ";
            case RIGHT -> " RIGHT JOIN ";
        };
    }


    private CriteriaElement criteriaElement$md() {
        return new CriteriaElement()
                .tableName("T_PAYMENT")
                .tableAlias("MD")
                .cluster(domainManager.getCluster(PaymentDomain.class))
                .shardType(ShardType.MULTI_SHARDABLE)
                .columns(5L)
                .joins(
                        Arrays.asList(
                                new CriteriaElementJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(1, 0))
                                        .element(criteriaElement$accDt()),
                                new CriteriaElementJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(3, 2))
                                        .element(criteriaElement$accCt()),
                                new CriteriaElementJoin()
                                        .joinType(JoinType.LEFT)
                                        .linked(false)
                                        .joinColumns(Pair.of(4, 5))
                                        .element(criteriaElement$extDoc())
                        )
                );
    }

    private CriteriaElement criteriaElement$accDt() {
        return new CriteriaElement()
                .tableName("T_ACCOUNT")
                .tableAlias("ACC_DT")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 3)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(7, 6))
                                        .element(criteriaElement$clDt())
                        )
                );
    }

    private CriteriaElement criteriaElement$clDt() {
        return new CriteriaElement()
                .tableName("T_CLIENT")
                .tableAlias("CL_DT")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 5);
    }

    private CriteriaElement criteriaElement$accCt() {
        return new CriteriaElement()
                .tableName("T_ACCOUNT")
                .tableAlias("ACC_CT")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 4)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(9, 8))
                                        .element(criteriaElement$clCt())
                        )
                );
    }

    private CriteriaElement criteriaElement$clCt() {
        return new CriteriaElement()
                .tableName("T_CLIENT")
                .tableAlias("CL_CT")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 6)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(false)
                                        .joinColumns(Pair.of(11, 10))
                                        .element(criteriaElement$clCat())
                        )
                );
    }

    private CriteriaElement criteriaElement$clCat() {
        return new CriteriaElement()
                .tableName("T_CLIENT_CATEGORY")
                .tableAlias("CL_CAT")
                .cluster(domainManager.getCluster(ClientCategoryDomain.class))
                .shardType(ShardType.REPLICABLE);
    }

    private CriteriaElement criteriaElement$extDoc() {
        return new CriteriaElement()
                .tableName("T_EXTERNAL_PAYMENT")
                .tableAlias("EXT_DOC")
                .cluster(domainManager.getCluster(ExternalPaymentDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 7);
    }

    @Override
    public Stream<PaymentCriteria> get(Object... binds) {
        return null;
    }
}
