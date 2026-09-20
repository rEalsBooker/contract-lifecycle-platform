package com.contractguard.platform.receivable;
import com.fasterxml.jackson.annotation.JsonFormat; import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDate;
public record CreateReceiptRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,
 @NotNull @JsonFormat(pattern="yyyy-MM-dd") LocalDate receiptDate, @NotBlank @Size(max=32) String paymentMethod,
 @NotBlank @Size(max=255) String payerName, @Size(max=128) String referenceNo, @Size(max=1000) String note,
 @NotBlank @Size(max=64) String requestKey) { }

