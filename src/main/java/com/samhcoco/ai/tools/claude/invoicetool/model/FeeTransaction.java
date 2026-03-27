package com.samhcoco.ai.tools.claude.invoicetool.model;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
@EqualsAndHashCode
public class FeeTransaction {
    private BigDecimal amount;
    private LocalDate date;
    private String company;
}
