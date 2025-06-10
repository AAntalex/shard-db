package com.antalex.db.dao.criteria;

import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.criteria.CriteriaElement;
import com.antalex.db.model.criteria.CriteriaElementJoin;
import com.antalex.db.model.criteria.CriteriaPredicate;
import com.antalex.db.model.criteria.CriteriaRoute;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Component
public class ExternalPaymentCriteria$RepositoryImpl3 implements CriteriaRepository<ExternalPaymentCriteria> {
    private static final List<String> COLUMNS = Arrays.asList(
            "MD.C_NUM",
            "MD.C_SUM",
            "MD.C_DATE",
            "ACC_DT.C_CODE",
            "ACC_CT.C_CODE",
            "CL_DT.C_NAME",
            "replace(cl_dt.c_name, ' ')",
            "CL_CT.C_NAME",
            "EXT_DOC.C_RECEIVER",
            "CL_CAT.ID"
    );

    private static final CriteriaElement ELEMENT_EXT_DOC = new CriteriaElement()
            .tableName("T_EXTERNAL_PAYMENT")
            .tableAlias("EXT_DOC")
            .shardType(ShardType.SHARDABLE)
            .index(0)
            .columns(256L);

    private static final CriteriaElement ELEMENT_MD = new CriteriaElement()
            .tableName("T_PAYMENT")
            .tableAlias("MD")
            .shardType(ShardType.MULTI_SHARDABLE)
            .index(1)
            .columns(7L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("MD.ID", "EXT_DOC.C_DOC"))
                            .element(ELEMENT_EXT_DOC)
            );

    private static final CriteriaElement ELEMENT_ACC_DT = new CriteriaElement()
            .tableName("T_ACCOUNT")
            .tableAlias("ACC_DT")
            .shardType(ShardType.SHARDABLE)
            .index(2)
            .columns(8L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("ACC_DT.ID", "MD.C_ACC_DT"))
                            .element(ELEMENT_MD)
            );

    private static final CriteriaElement ELEMENT_ACC_CT = new CriteriaElement()
            .tableName("T_ACCOUNT")
            .tableAlias("ACC_CT")
            .shardType(ShardType.SHARDABLE)
            .index(3)
            .columns(16L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("ACC_CT.ID", "MD.C_ACC_CT"))
                            .element(ELEMENT_MD)
            );

    private static final CriteriaElement ELEMENT_CL_CT = new CriteriaElement()
            .tableName("T_CLIENT")
            .tableAlias("CL_CT")
            .shardType(ShardType.SHARDABLE)
            .index(4)
            .columns(128L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("CL_CT.ID", "ACC_CT.C_CLIENT"))
                            .element(ELEMENT_ACC_CT)
            );

    private static final CriteriaElement ELEMENT_CL_DT = new CriteriaElement()
            .tableName("T_CLIENT")
            .tableAlias("CL_DT")
            .shardType(ShardType.SHARDABLE)
            .index(5)
            .columns(96L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(true)
                            .joinColumns(Pair.of("CL_DT.ID", "ACC_DT.C_CLIENT"))
                            .element(ELEMENT_ACC_DT)
            );

    private static final CriteriaElement ELEMENT_CL_CAT = new CriteriaElement()
            .tableName("T_CLIENT_CATEGORY")
            .tableAlias("CL_CAT")
            .shardType(ShardType.REPLICABLE)
            .index(6)
            .columns(512L)
            .join(
                    new CriteriaElementJoin()
                            .joinType(JoinType.INNER)
                            .linkedShard(false)
                            .joinColumns(Pair.of("CL_CAT.ID", "CL_CT.C_CATEGORY"))
                            .element(ELEMENT_CL_CT)
            );

    private static final List<CriteriaElement> ELEMENTS = Arrays.asList(
            ELEMENT_EXT_DOC,
            ELEMENT_MD,
            ELEMENT_ACC_DT,
            ELEMENT_ACC_CT,
            ELEMENT_CL_CT,
            ELEMENT_CL_DT,
            ELEMENT_CL_CAT
    );

    private static final List<PredicateGroup> PREDICATE_GROUPS = Arrays.asList(
            new PredicateGroup(15L, 7L),
            new PredicateGroup(19L, 3L)
    );

    private static final List<CriteriaPredicate> PREDICATES = Arrays.asList(
            new CriteriaPredicate()
                    .value("EXT_DOC.C_DATE<{:1}")
                    .aliasMask(1L),
            new CriteriaPredicate()
                    .value("EXT_DOC.C_RECEIVER IS NULL")
                    .aliasMask(1L),
            new CriteriaPredicate()
                    .value("MD.C_DATE_PROV<{:2}")
                    .aliasMask(2L),
            new CriteriaPredicate()
                    .value("CL_CAT.C_CODE='VIP'")
                    .aliasMask(64L),
            new CriteriaPredicate()
                    .value("ACC_DT.C_CODE LIKE '40702810%3'")
                    .aliasMask(4L)
    );

    private final ShardDataBaseManager dataBaseManager;
    private final ShardEntityManager entityManager;

    @Autowired
    ExternalPaymentCriteria$RepositoryImpl3(ShardEntityManager entityManager, ShardDataBaseManager dataBaseManager) {
        this.dataBaseManager = dataBaseManager;
        this.entityManager = entityManager;

        ELEMENT_EXT_DOC.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_MD.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_ACC_DT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_ACC_CT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_CT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_DT.cluster(dataBaseManager.getCluster("DEFAULT"));
        ELEMENT_CL_CAT.cluster(dataBaseManager.getCluster("DEFAULT"));
    }

    @Override
    public Stream<ExternalPaymentCriteria> get(Object... binds) {
        return null;
    }

    public List<CriteriaRoute> getCriteriaRoutes(PredicateGroup predicateGroup) {
        return null;
    }
}
