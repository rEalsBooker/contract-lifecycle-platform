package com.contractguard.platform.organization;
import jakarta.validation.constraints.NotEmpty; import java.util.List;
public record UpdateRolesRequest(@NotEmpty List<String> roleCodes){}

