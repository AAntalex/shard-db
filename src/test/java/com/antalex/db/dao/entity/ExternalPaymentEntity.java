package com.antalex.db.dao.entity;

import com.antalex.db.annotation.ShardEntity;
import com.antalex.db.entity.abstraction.BaseShardEntity;
import com.antalex.db.model.enums.ShardType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Date;

@EqualsAndHashCode(callSuper = true)
@Table(
       indexes = {
               @Index(columnList = "date"),
               @Index(columnList = "doc")
        })
@Data
@Accessors(chain = true, fluent = true)
@ShardEntity(type = ShardType.SHARDABLE)
public class ExternalPaymentEntity extends BaseShardEntity {
    private String receiver;
    private Date date;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn
    private PaymentEntity doc;
}
