package com.contractguard.platform.contract;

import java.math.BigDecimal;

public record CreateContractDraftResponse(Long id, String contractNo, String name, String businessStatus,
                                          BigDecimal totalAmount, String sourceFilename) { }

