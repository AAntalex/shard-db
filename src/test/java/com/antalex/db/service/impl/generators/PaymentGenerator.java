package com.antalex.db.service.impl.generators;

import com.antalex.db.dao.domain.AccountDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.dao.domain.PaymentDomain;
import com.antalex.db.service.DataGeneratorService;
import com.antalex.db.service.DomainEntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
public class PaymentGenerator implements DataGeneratorService<PaymentDomain> {
    @Autowired
    private DomainEntityManager domainManager;
    @Autowired
    private AccountGenerator accountGenerator;

    private static final String ACCOUNT_PREFIX = "40702810X";

    @Override
    public List<PaymentDomain> generate(int count) {
        List<AccountDomain> accounts =
                accountGenerator.generate(count / 10)
                        .stream()
                        .sorted(Comparator.comparing(AccountDomain::code))
                        .toList();
        int cntAccount = accounts.size();
        List<PaymentDomain> payments = domainManager.findAll(PaymentDomain.class);
        payments.addAll(
                IntStream.rangeClosed(payments.size() + 1, count)
                        .mapToObj(idx ->
                                domainManager.newDomain(PaymentDomain.class)
                                        .num(idx)
                                        .sum(new BigDecimal("1.01"))
                                        .date(new Date())
                                        .dateProv(OffsetDateTime.now())
                                        .accDt(accounts.get(ThreadLocalRandom.current().nextInt(0, cntAccount)))
                                        .accCt(accounts.get(ThreadLocalRandom.current().nextInt(0, cntAccount)))
                        )
                        .toList()
        );
        domainManager.updateAll(payments);
        return payments;
    }
}
