package com.antalex.db.service.impl.generators;

import com.antalex.db.dao.domain.AccountDomain;
import com.antalex.db.dao.domain.ClientDomain;
import com.antalex.db.service.DataGeneratorService;
import com.antalex.db.service.DomainEntityManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@Component
public class AccountGenerator implements DataGeneratorService<AccountDomain> {
    @Autowired
    private DomainEntityManager domainManager;
    @Autowired
    private ClientGenerator clientGenerator;

    private static final int ADD_COUNT = 10;
    private static final String ACCOUNT_PREFIX = "40702810X";

    @Override
    public List<AccountDomain> generate(int count) {
        List<ClientDomain> clients =
                clientGenerator.generate(count / 10 + ADD_COUNT)
                        .stream()
                        .sorted(Comparator.comparing(ClientDomain::name))
                        .toList();
        int clientCount = clients.size() - ADD_COUNT;
        List<AccountDomain> accounts = domainManager.findAll(AccountDomain.class);
        accounts.addAll(
                IntStream.rangeClosed(accounts.size() + 1, count)
                        .mapToObj(idx ->
                                domainManager.newDomain(AccountDomain.class)
                                        .code(
                                                ACCOUNT_PREFIX +
                                                        StringUtils.leftPad(
                                                                String.valueOf(idx),
                                                                20 - ACCOUNT_PREFIX.length(),
                                                                '0')
                                        )
                                        .balance(BigDecimal.ZERO)
                                        .dateOpen(OffsetDateTime.now())
                                        .client(clients.get(ThreadLocalRandom.current().nextInt(0, clientCount)))
                        )
                        .toList()
        );
        domainManager.updateAll(accounts);
        return accounts;
    }
}
