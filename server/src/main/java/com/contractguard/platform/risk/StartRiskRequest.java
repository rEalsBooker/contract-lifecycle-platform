package com.contractguard.platform.risk;
import jakarta.validation.constraints.*;
public record StartRiskRequest(@NotNull Long handlerMembershipId,@NotBlank @Size(max=1000) String handlingPlan) { }

