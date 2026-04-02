package com.samhcoco.ai.tools.claude.invoicetool.repository;

import com.samhcoco.ai.tools.claude.invoicetool.model.FeeTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FeeTransactionRepository extends JpaRepository<FeeTransaction, Long> {
}
