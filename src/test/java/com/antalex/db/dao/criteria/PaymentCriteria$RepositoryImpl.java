package com.antalex.db.dao.criteria;

import com.antalex.db.dao.domain.PaymentDomain;
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
import java.util.List;
import java.util.stream.Stream;

@Component
public class PaymentCriteria$RepositoryImpl implements CriteriaRepository<PaymentCriteria> {
    private static final ShardType SHARD_TYPE = ShardType.REPLICABLE;

    private final DomainEntityManager domainManager;
    private CriteriaElement mainElement;

    @Autowired
    PaymentCriteria$RepositoryImpl(DomainEntityManager domainManager) {
        this.domainManager = domainManager;
        this.mainElement = getCriteriaElement$1();
    }

    private CriteriaElement getCriteriaElement$1() {
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
                                        .element(getCriteriaElement$2()),
                                new CriteriaJoin()
                                        .joinType(JoinType.INNER)
                                        .linked(true)
                                        .joinColumns(Pair.of("C_ACC_CT", "ID"))
                                        .element(getCriteriaElement$3())
                        )
                );
    }

    private CriteriaElement getCriteriaElement$2() {
        return null;
    }

    private CriteriaElement getCriteriaElement$3() {
        return null;
    }
    @Override
    public Stream<PaymentCriteria> get(Object... binds) {
        return null;
    }
}
