package com.antalex.db.dao.criteria;

import com.antalex.db.model.PredicateGroup;
import com.antalex.db.model.criteria.*;
import com.antalex.db.model.enums.ShardType;
import com.antalex.db.service.CriteriaRepository;
import com.antalex.db.service.ShardDataBaseManager;
import com.antalex.db.service.ShardEntityManager;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.criteria.JoinType;
import java.util.*;
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
        List<CriteriaRoute> criteriaRoutes = new ArrayList<>();


        return criteriaRoutes;
    }

    private static void fillCriteriaParts(Map<Long, CriteriaPart> criteriaParts, CriteriaElement element) {
        Map<Integer, CriteriaPartRelation> relations = new HashMap<>();
        if (element != null) {
            getRelation(element, relations);
        }
        ELEMENTS.forEach(el -> getRelation(el, relations));
        relations
                .values()
                .stream()
                .map(CriteriaPartRelation::part)
                .forEach(part -> criteriaParts.putIfAbsent(part.aliasMask(), part));
    }

    private static @NotNull CriteriaPartRelation getRelation(
            CriteriaElement element,
            Map<Integer, CriteriaPartRelation> relations)
    {
        return Optional
                .ofNullable(relations.get(element.index()))
                .orElseGet(() -> {
                    CriteriaPartRelation relation =
                            new CriteriaPartRelation()
                                    .part(getCriteriaPart(element, relations))
                                    .joinColumn(
                                            Optional
                                                    .ofNullable(element.join())
                                                    .map(CriteriaElementJoin::joinColumns)
                                                    .map(Pair::getLeft)
                                                    .orElse(null)
                                    );
                    relations.put(element.index(), relation);
                    return relation;
                });
    }

    private static boolean alienPart(CriteriaElement element, CriteriaElementJoin join, CriteriaPartRelation relation) {
        return join.linkedShard() &&
                (join.element().shardType() != ShardType.SHARDABLE ||
                        element.shardType() != ShardType.SHARDABLE) &&
                Optional
                        .ofNullable(relation.joinColumn())
                        .map(column -> !column.equals(join.joinColumns().getRight()))
                        .orElse(false);
    }

    private static @NotNull CriteriaPart getCriteriaPart(
            CriteriaElement element,
            Map<Integer, CriteriaPartRelation> relations)
    {
        return Optional
                .ofNullable(element.join())
                .map(join -> {
                    CriteriaPartRelation parentRelation = getRelation(join.element(), relations);
                    CriteriaPart criteriaPart = parentRelation.part();
                    boolean isAlienPart = alienPart(element, join, parentRelation);
                    if (join.linkedShard() && !isAlienPart ||
                            join.element().cluster() == element.cluster() && element.cluster().getShards().size() == 1
                    ) {
                        if (parentRelation.joinColumn() == null) {
                            parentRelation.joinColumn(join.joinColumns().getRight());
                        }
                        return criteriaPart
                                .aliasMask(criteriaPart.aliasMask() | 1L << element.index())
                                .columns(criteriaPart.columns() | element.columns())
                                .from(criteriaPart.from() + getJoinText(join.joinType()) + getTableName(element) + getOn(join));
                    }
                    if (isAlienPart) {

                    }
                    CriteriaPart childPart = createCriteriaPart(element);
                    criteriaPart
                            .joins()
                            .add(
                                    new CriteriaPartJoin()
                                            .part(childPart)
                                            .joinType(join.joinType())
                                            .joinColumns(join.joinColumns())
                            );
                    return childPart;
                })
                .orElseGet(() -> createCriteriaPart(element));
    }

    private static CriteriaPart createCriteriaPart(CriteriaElement element) {
        return new CriteriaPart()
                .aliasMask(1L << element.index())
                .columns(element.columns())
                .from(getTableName(element));
    }

    private static String getTableName(CriteriaElement element) {
        return element.tableName() + " " + element.tableAlias();
    }

    private static String getOn(CriteriaElementJoin join) {
        return " ON " + join.joinColumns().getLeft() + "=" + join.joinColumns().getRight();
    }
    private static String getJoinText(JoinType joinType) {
        return switch (joinType) {
            case INNER -> " JOIN ";
            case LEFT -> " LEFT JOIN ";
            case RIGHT -> " RIGHT JOIN ";
        };
    }

}
