package com.contractguard.platform.ai.assistant;
import com.contractguard.platform.ai.config.*;
import com.contractguard.platform.ai.extraction.*;
import com.contractguard.platform.ai.rag.*;
import com.contractguard.platform.ai.agent.*;
import com.contractguard.platform.security.AuthPrincipal; import jakarta.validation.Valid; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/contracts/{contractId}/ai") public class ContractAssistantController { private final ContractAssistantService service; public ContractAssistantController(ContractAssistantService service){this.service=service;} @PostMapping("/questions") public ContractAnswerResponse ask(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long contractId,@Valid @RequestBody AskContractQuestionRequest request){return service.ask(p,contractId,request.question());} }


