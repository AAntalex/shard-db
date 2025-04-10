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
import java.util.stream.Stream;

@Component
public class PaymentCriteria$RepositoryImpl implements CriteriaRepository<PaymentCriteria> {
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;

    private final DomainEntityManager domainManager;
    private CriteriaElement mainElement;

    @Autowired
    PaymentCriteria$RepositoryImpl(DomainEntityManager domainManager) {
        this.domainManager = domainManager;
        this.mainElement = getCriteriaElement$md();
    }

    private CriteriaElement getCriteriaElement$md() {
        return new CriteriaElement()
                .tableName("T_PAYMENT")
                .tableAlias("md")
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
                                        .element(getCriteriaElement$accDt()),
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_ACC_CT", "ID"))
                                        .element(getCriteriaElement$accCt()),
                                new CriteriaJoin()
                                        .joinType(JoinType.LEFT)
                                        .linked(false)
                                        .joinColumns(Pair.of("ID", "C_DOC"))
                                        .element(getCriteriaElement$extDoc())
                        )
                );
    }

    private CriteriaElement getCriteriaElement$accDt() {
        return new CriteriaElement()
                .tableName("T_ACCOUNT")
                .tableAlias("acc_dt")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_CODE"
                        )
                )
                .joins(
                        Arrays.asList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_CLIENT", "ID"))
                                        .element(getCriteriaElement$clDt())
                        )
                );
    }

    private CriteriaElement getCriteriaElement$clDt() {
        return new CriteriaElement()
                .tableName("T_CLIENT")
                .tableAlias("cl_dt")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_NAME"
                        )
                );
    }

    private CriteriaElement getCriteriaElement$accCt() {
        return new CriteriaElement()
                .tableName("T_ACCOUNT")
                .tableAlias("acc_ct")
                .cluster(domainManager.getCluster(AccountDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_CODE"
                        )
                )
                .joins(
                        Arrays.asList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_CLIENT", "ID"))
                                        .element(getCriteriaElement$clCt())
                        )
                );
    }

    private CriteriaElement getCriteriaElement$clCt() {
        return new CriteriaElement()
                .tableName("T_CLIENT")
                .tableAlias("cl_ct")
                .cluster(domainManager.getCluster(ClientDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_NAME"
                        )
                ).joins(
                        Arrays.asList(
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(false)
                                        .joinColumns(Pair.of("C_CATEGORY", "ID"))
                                        .element(getCriteriaElement$clCat())
                        )
                );
    }

    private CriteriaElement getCriteriaElement$clCat() {
        return new CriteriaElement()
                .tableName("T_CLIENT_CATEGORY")
                .tableAlias("cl_cat")
                .cluster(domainManager.getCluster(ClientCategoryDomain.class))
                .shardType(ShardType.REPLICABLE);
    }

    private CriteriaElement getCriteriaElement$extDoc() {
        return new CriteriaElement()
                .tableName("T_EXTERNAL_PAYMENT")
                .tableAlias("ext_doc")
                .cluster(domainManager.getCluster(ExternalPaymentDomain.class))
                .shardType(ShardType.SHARDABLE)
                .columns(
                        Arrays.asList(
                                "C_RECEIVER"
                        )
                );
    }

    @Override
    public Stream<PaymentCriteria> get(Object... binds) {
        return null;
    }
}
