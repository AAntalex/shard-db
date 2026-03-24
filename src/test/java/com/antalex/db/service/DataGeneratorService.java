package com.antalex.db.service;


import com.antalex.db.domain.abstraction.Domain;

import java.util.List;

public interface DataGeneratorService<T extends Domain> {
    List<T> generate(int count);
}
