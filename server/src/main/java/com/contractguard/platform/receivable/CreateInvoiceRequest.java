package com.contractguard.platform.receivable;
import com.fasterxml.jackson.annotation.JsonFormat; import jakarta.validation.constraints.*;
import java.math.BigDecimal; import java.time.LocalDate;
public record CreateInvoiceRequest(@NotBlank @Size(max=128) String invoiceNo, @NotNull @DecimalMin("0.01") BigDecimal amount,
 @NotNull @JsonFormat(pattern="yyyy-MM-dd") LocalDate invoiceDate, @NotBlank @Size(max=64) String invoiceType,
 @NotBlank @Size(max=255) String invoiceTitle, @NotBlank @Size(max=64) String requestKey) { }

