package com.contractguard.platform.fulfillment;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record ClaimTaskRequest(@NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dueDate, @NotNull Long reviewerMembershipId) { }

