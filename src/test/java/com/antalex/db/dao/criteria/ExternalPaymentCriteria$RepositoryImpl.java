package com.antalex.db.dao.criteria;

import com.antalex.db.dao.domain.*;
import com.antalex.db.dao.entity.ExternalPaymentEntity;
import com.antalex.db.dao.entity.PaymentEntity;
import com.antalex.db.model.criteria.*;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.*;
import java.util.stream.Stream;

@Component
public class ExternalPaymentCriteria$RepositoryImpl implements CriteriaRepository<PaymentCriteria> {
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

    private static final CriteriaElement ELEMENT_EXT_DOC = new CriteriaElement()
            .tableName("T_EXTERNAL_PAYMENT")
            .tableAlias("EXT_DOC")
            .shardType(ShardType.SHARDABLE)
            .columns(1L << 7);

    private static final CriteriaElement ELEMENT_MD = new CriteriaElement()
            .tableName("T_PAYMENT")
            .tableAlias("MD")
            .shardType(ShardType.MULTI_SHARDABLE)
            .columns(5L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linked(true)
                            .joinColumns(Pair.of(5, 4))
                            .element(ELEMENT_EXT_DOC)
            );

    private static final List<CriteriaElement> ELEMENTS = Arrays.asList(
            ELEMENT_EXT_DOC,
            ELEMENT_MD
    );

    private final ShardDataBaseManager dataBaseManager;
    private final ShardEntityManager entityManager;


    private List<CriteriaPart> criteriaRoutes = new ArrayList<>();

    @Autowired
    ExternalPaymentCriteria$RepositoryImpl(ShardEntityManager entityManager, ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
        this.entityManager = entityManager;

        ELEMENT_EXT_DOC.cluster(entityManager.getCluster(ExternalPaymentEntity.class));
        ELEMENT_MD.cluster(entityManager.getCluster(PaymentEntity.class));
    }


    private String getTableName(CriteriaElement2 element) {
        return element.tableName() + " " + element.tableAlias();
    }

    private String getOn(CriteriaElementJoin2 join) {
        return " ON " +
                Optional
                        .ofNullable(join.on())
                        .orElseGet(() ->
                                JOIN_COLUMNS.get(join.joinColumns().getLeft()) + "="
                                        + JOIN_COLUMNS.get(join.joinColumns().getRight())
                        );
    }



    private String getJoinText(JoinType joinType) {
        return switch (joinType) {
            case INNER -> " JOIN ";
            case LEFT -> " LEFT JOIN ";
            case RIGHT -> " RIGHT JOIN ";
        };
    }

    private CriteriaElement criteriaElement$extDoc() {
        return new CriteriaElement()
                .tableName("T_EXTERNAL_PAYMENT")
                .tableAlias("EXT_DOC")
                .cluster(domainManager.getCluster(ExternalPaymentDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 7);
    }

    private CriteriaElement criteriaElement$md() {
        return new CriteriaElement()
                .tableName("T_PAYMENT")
                .tableAlias("MD")
                .cluster(domainManager.getCluster(PaymentDomain.class))
                .shardType(ShardType.MULTI_SHARDABLE)
                .columns(5L)
                .join(
                        new CriteriaElementJoin()
                                .joinType(JoinType.INNER)
                                .linked(true)
                                .joinColumns(Pair.of(5, 4))
                                .element(criteriaElement$extDoc())
                        );
    }

    private CriteriaElement2 criteriaElement$accDt() {
        return new CriteriaElement2()
                .tableName("T_ACCOUNT")
                .tableAlias("ACC_DT")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 3)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin2()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(7, 6))
                                        .element(criteriaElement$clDt())
                        )
                );
    }

    private CriteriaElement2 criteriaElement$clDt() {
        return new CriteriaElement2()
                .tableName("T_CLIENT")
                .tableAlias("CL_DT")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 5);
    }

    private CriteriaElement2 criteriaElement$accCt() {
        return new CriteriaElement2()
                .tableName("T_ACCOUNT")
                .tableAlias("ACC_CT")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 4)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin2()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of(9, 8))
                                        .element(criteriaElement$clCt())
                        )
                );
    }

    private CriteriaElement2 criteriaElement$clCt() {
        return new CriteriaElement2()
                .tableName("T_CLIENT")
                .tableAlias("CL_CT")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(1L << 6)
                .joins(
                        Collections.singletonList(
                                new CriteriaElementJoin2()
                                        .joinType(JoinType.INNER)
                                        .linked(false)
                                        .joinColumns(Pair.of(11, 10))
                                        .element(criteriaElement$clCat())
                        )
                );
    }

    private CriteriaElement2 criteriaElement$clCat() {
        return new CriteriaElement2()
                .tableName("T_CLIENT_CATEGORY")
                .tableAlias("CL_CAT")
                .cluster(domainManager.getCluster(ClientCategoryDomain.class))
                .shardType(ShardType.REPLICABLE);
    }


    @Override
    public Stream<PaymentCriteria> get(Object... binds) {
        return null;
    }
}
