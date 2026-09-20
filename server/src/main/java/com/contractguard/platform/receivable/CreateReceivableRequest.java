package com.contractguard.platform.receivable;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDate;
public record CreateReceivableRequest(@NotNull Long contractId, @NotBlank @Size(max=255) String planName,
 @NotNull @DecimalMin("0.01") BigDecimal planAmount, @NotBlank String triggerStatus,
 @JsonFormat(pattern="yyyy-MM-dd") LocalDate dueDate) { }

