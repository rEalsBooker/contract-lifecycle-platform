package com.contractguard.platform.organization;
import jakarta.validation.constraints.*; import java.util.List;
public record CreateMemberRequest(@NotBlank @Size(max=64) String username,@NotBlank @Size(max=64) String displayName,@NotBlank @Size(min=8,max=72) String initialPassword,@NotEmpty List<String> roleCodes){}

