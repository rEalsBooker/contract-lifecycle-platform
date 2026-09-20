package com.contractguard.platform.risk;
import com.fasterxml.jackson.annotation.JsonFormat; import jakarta.validation.constraints.*; import java.time.LocalDate;
public record IgnoreRiskRequest(@NotBlank @Size(max=1000) String reason,@NotNull @JsonFormat(pattern="yyyy-MM-dd") LocalDate reviewDate) { }

