package com.antalex.db.dao.domain;

import com.antalex.db.annotation.Attribute;
import com.antalex.db.annotation.DomainEntity;
import com.antalex.db.annotation.Storage;
import com.antalex.db.dao.entity.ExternalPaymentEntity;
import com.antalex.db.domain.abstraction.BaseDomain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.FetchType;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true, fluent = true)
@DomainEntity(
        value = ExternalPaymentEntity.class,
        storage = @Storage(fetchType = FetchType.LAZY)
)
public class ExternalPaymentDomain extends BaseDomain {
    @Attribute
    private String receiver;
    @Attribute
    private Date date;
    @Attribute
    private PaymentDomain doc;
}
