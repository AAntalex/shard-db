package com.antalex.db.dao.domain;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;

@Data
@Accessors(chain = true, fluent = true)
public class Contract {
    private String description;
    private LocalDate date;
}
