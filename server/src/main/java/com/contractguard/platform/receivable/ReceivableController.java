package com.contractguard.platform.receivable;
import com.contractguard.platform.security.AuthPrincipal; import jakarta.validation.Valid; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*; import java.util.List;
@RestController @RequestMapping("/api/v1")
public class ReceivableController { private final ReceivableService service; public ReceivableController(ReceivableService service){this.service=service;}
 @GetMapping("/receivables") public List<ReceivableResponse> list(@AuthenticationPrincipal AuthPrincipal p){return service.list(p);}
 @PostMapping("/receivables") public ReceivableResponse create(@AuthenticationPrincipal AuthPrincipal p,@Valid @RequestBody CreateReceivableRequest r){return service.createPlan(p,r);}
 @PostMapping("/receivables/{id}/receipts") public ReceivableResponse receipt(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id,@Valid @RequestBody CreateReceiptRequest r){return service.addReceipt(p,id,r);}
 @PostMapping("/receivables/{id}/invoices") public ReceivableResponse invoice(@AuthenticationPrincipal AuthPrincipal p,@PathVariable Long id,@Valid @RequestBody CreateInvoiceRequest r){return service.addInvoice(p,id,r);}
}

