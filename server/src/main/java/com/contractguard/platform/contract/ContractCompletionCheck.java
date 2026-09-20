package com.contractguard.platform.contract;
import java.math.BigDecimal;
public record ContractCompletionCheck(long unfinishedTaskCount,long pendingReviewCount,long activeRiskCount,BigDecimal receivableBalance,boolean completable){}

