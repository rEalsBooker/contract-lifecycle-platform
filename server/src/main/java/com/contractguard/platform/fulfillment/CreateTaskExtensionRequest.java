package com.contractguard.platform.fulfillment;
import jakarta.validation.constraints.*; import java.time.LocalDate;
public record CreateTaskExtensionRequest(@NotNull LocalDate requestedInternalPlanDate,@NotBlank @Size(max=1000) String reason,@NotNull Long approverMembershipId){}

