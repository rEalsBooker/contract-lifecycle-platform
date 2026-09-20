package com.contractguard.platform.fulfillment;
import jakarta.validation.constraints.*;
public record ReviewTaskExtensionRequest(boolean approved,@Size(max=1000) String note){}

