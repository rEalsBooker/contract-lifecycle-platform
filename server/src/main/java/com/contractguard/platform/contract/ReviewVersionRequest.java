package com.contractguard.platform.contract;
import jakarta.validation.constraints.Size;
public record ReviewVersionRequest(boolean approved,@Size(max=1000) String note){}

