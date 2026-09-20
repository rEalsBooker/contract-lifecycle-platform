package com.contractguard.platform.contract;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateClauseRequest(@NotBlank String clauseType, @NotBlank String clauseTitle,
                                  @NotBlank String clauseContent, @NotNull Integer sourcePageNo,
                                  String sourceExcerpt) { }

