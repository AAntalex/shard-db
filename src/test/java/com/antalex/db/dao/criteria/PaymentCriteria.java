package com.antalex.db.dao.criteria;

import com.antalex.db.annotation.CachePolicy;
import com.antalex.db.annotation.Criteria;
import com.antalex.db.annotation.CriteriaAttribute;
import com.antalex.db.annotation.Join;
import com.antalex.db.dao.entity.*;
import com.antalex.db.service.impl.managers.TransactionalCacheManager;
import lombok.Data;

import javax.persistence.FetchType;
import javax.persistence.criteria.JoinType;
import java.math.BigDecimal;
import java.util.Date;

@Data

@Criteria(
        from = PaymentEntity.class,
        alias = "md",
        joins = {
                @Join(from = AccountEntity.class, alias = "acc_dt", on = "${acc_dt} = ${md.accDt}"),
                @Join(from = AccountEntity.class, alias = "acc_ct", on = "${id} = ${md.accCt}"),
                @Join(
                        from = ExternalPaymentEntity.class,
                        alias = "ext_doc",
                        on = "${doc} = ${md.id}",
                        joinType = JoinType.LEFT
                ),
                @Join(from = ClientEntity.class, alias = "cl_ct", on = "${id} = ${acc_ct.client}"),
                @Join(from = ClientEntity.class, alias = "cl_dt", on = "${id} = ${acc_dt.client}"),
                @Join(from = ClientCategoryEntity.class, alias = "cl_cat", on = "${id} = ${cl_ct.category}"),
        },
        where = "${ext_doc.date} >= ? and ({md.dateProv} >= ? and ${cl_cat.code} = 'VIP' or {acc_dt.code} like '40702810%3')",
        cachePolicy = @CachePolicy(
                fetch = FetchType.EAGER,
                implement = TransactionalCacheManager.class,
                key = {"md.num", "md.date"},
                retentionTime = 60
        )
)
public class PaymentCriteria {
    @CriteriaAttribute("${md.num}")
    private Integer num;
    @CriteriaAttribute("${md.sum}")
    private BigDecimal sum;
    @CriteriaAttribute("${md.date}")
    private Date date;
    @CriteriaAttribute("acc_dt.code")
    private String dtNum;
    @CriteriaAttribute("acc_ct.code")
    private String ctNum;
    @CriteriaAttribute("cl_dt.name")
    private String dtClientName;
    @CriteriaAttribute("cl_ct.name")
    private String ctClientName;
    @CriteriaAttribute("ext_doc.receiver")
    private String receiver;
}
