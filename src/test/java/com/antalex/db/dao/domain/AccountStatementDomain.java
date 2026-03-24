package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.dao.entity.AccountStatementEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(AccountStatementEntity.class)
public class AccountStatementDomain extends BaseDomain {
    @Attribute
    private PaymentDomain payment;
    @Attribute
    private BigDecimal sum;
    @Attribute
    private OffsetDateTime date;
}
