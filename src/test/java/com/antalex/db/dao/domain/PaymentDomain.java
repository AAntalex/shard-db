package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.Storage;
import com.antalex.db.dao.entity.PaymentEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.FetchType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(
        value = PaymentEntity.class,
        storage = @Storage(fetchType = FetchType.LAZY)
)
public class PaymentDomain extends BaseDomain {
    @Attribute
    private Integer num;
    @Attribute
    private BigDecimal sum;
    @Attribute
    private Date date;
    @Attribute
    private OffsetDateTime dateProv;
    @Attribute
    private AccountDomain accDt;
    @Attribute
    private AccountDomain accCt;
}
