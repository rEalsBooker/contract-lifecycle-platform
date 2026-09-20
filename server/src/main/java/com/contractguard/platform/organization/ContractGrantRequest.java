package com.contractguard.platform.organization;
import jakarta.validation.constraints.NotNull;
public record ContractGrantRequest(@NotNull Long contractId,@NotNull Long membershipId){}

