package com.antalex.db.dao.criteria;

import com.antalex.db.annotation.CachePolicy;
import com.antalex.db.annotation.Criteria;
import com.antalex.db.annotation.CriteriaAttribute;
import com.antalex.db.annotation.Join;
import com.antalex.db.dao.domain.ClientCategoryDomain;
import com.antalex.db.dao.entity.*;
import com.antalex.db.service.impl.managers.TransactionalCacheManager;
import lombok.Data;

import javax.persistence.FetchType;
import javax.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.Date;

@Data

@Criteria(
        from = ExternalPaymentEntity.class,
        alias = "ext_doc",
        joins = {
                @Join(from = PaymentEntity.class, alias = "md", on = "ext_doc.doc = md"),
                @Join(from = AccountEntity.class, alias = "acc_dt", on = "acc_dt = md.accDt"),
                @Join(
                        from = AccountEntity.class,
                        alias = "acc_ct",
                        on = "acc_ct = md.accCt",
                        joinType = JoinType.LEFT
                ),
                @Join(
                        from = ClientEntity.class,
                        alias = "cl_ct",
                        on = "cl_ct = acc_ct.client",
                        joinType = JoinType.LEFT
                ),
                @Join(from = ClientEntity.class, alias = "cl_dt", on = "id = acc_dt.client"),
                @Join(
                        from = ClientCategoryEntity.class,
                        alias = "cl_cat",
                        on = "cl_cat = cl_ct.category",
                        joinType = JoinType.LEFT
                ),
        },
        where = "${ext_doc.date} >= ? and not ${receiver} is NULL and (${md.dateProv} >= ? and " +
                "${cl_cat.code} = 'VIP' or ${acc_dt.code} like '40702810%3')",
        cachePolicy = @CachePolicy(
                fetch = FetchType.EAGER,
                implement = TransactionalCacheManager.class,
                key = {"md.num", "md.date"},
                retentionTime = 60,
                refreshTime = 10
        )
)
public class ExternalPaymentCriteria {
    @CriteriaAttribute("md.num")
    private Integer num;
    @CriteriaAttribute("md.sum")
    private BigDecimal sum;
    @CriteriaAttribute("md.date")
    private Date date;
    @CriteriaAttribute("acc_dt.code")
    private String dtNum;
    @CriteriaAttribute("acc_ct.code")
    private String ctNum;
    @CriteriaAttribute("cl_dt.name")
    private String dtClientName;
    @CriteriaAttribute("replace(cl_dt.c_name, ' ')")
    private String normName;
    @CriteriaAttribute("cl_ct.name")
    private String ctClientName;
    @CriteriaAttribute("receiver")
    private String receiver;
    @CriteriaAttribute("cl_cat")
    private ClientCategoryDomain clientCategory;
}
