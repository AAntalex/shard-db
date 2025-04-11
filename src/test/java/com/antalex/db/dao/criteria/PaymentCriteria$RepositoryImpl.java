package com.antalex.db.dao.criteria;

import com.antalex.db.dao.domain.*;
import com.antalex.db.model.criteria.CriteriaElement;
import com.antalex.db.model.criteria.CriteriaJoin;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.DomainEntityManager;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Stream;

@Component
public class PaymentCriteria$RepositoryImpl implements CriteriaRepository<PaymentCriteria> {
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;

    private final DomainEntityManager domainManager;
    private CriteriaElement mainElement;

    @Autowired
    PaymentCriteria$RepositoryImpl(DomainEntityManager domainManager) {
        this.domainManager = domainManager;
        this.mainElement = criteriaElement$md();
    }

    private CriteriaElement criteriaElement$md() {
        return new CriteriaElement()
                .tableName("T_PAYMENT")
                .tableAlias("MD")
                .cluster(domainManager.getCluster(PaymentDomain.class))
                .shardType(ShardType.MULTI_SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_NUM",
                                "C_SUM",
                                "C_DATE"
                        )
                )
                .joins(
                        Arrays.asList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_ACC_DT", "ID"))
                                        .element(criteriaElement$accDt()),
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_ACC_CT", "ID"))
                                        .element(criteriaElement$accCt()),
                                new CriteriaJoin()
                                        .joinType(JoinType.LEFT)
                                        .linked(false)
                                        .joinColumns(Pair.of("ID", "C_DOC"))
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
                .columns(
                        Collections.singletonList(
                                "C_CODE"
                        )
                )
                .joins(
                        Collections.singletonList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_CLIENT", "ID"))
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
                .columns(
                        Collections.singletonList(
                                "C_NAME"
                        )
                );
    }

    private CriteriaElement criteriaElement$accCt() {
        return new CriteriaElement()
                .tableName("T_ACCOUNT")
                .tableAlias("ACC_CT")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Collections.singletonList(
                                "C_CODE"
                        )
                )
                .joins(
                        Collections.singletonList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_CLIENT", "ID"))
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
                .columns(
                        Collections.singletonList(
                                "C_NAME"
                        )
                ).joins(
                        Collections.singletonList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(false)
                                        .joinColumns(Pair.of("C_CATEGORY", "ID"))
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
                .columns(
                        Collections.singletonList(
                                "C_RECEIVER"
                        )
                );
    }

    @Override
    public Stream<PaymentCriteria> get(Object... binds) {
        return null;
    }
}
