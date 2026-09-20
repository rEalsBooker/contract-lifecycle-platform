package com.contractguard.platform.receivable;

import org.apache.ibatis.annotations.*;
import java.math.BigDecimal; import java.time.LocalDate; import java.util.List;

@Mapper
public interface ReceivableMapper {
    String BASE_SELECT = """
      SELECT rp.id,rp.contract_id,c.contract_no,c.name AS contract_name,c.counterparty_name,rp.plan_name,rp.plan_amount,
        rp.trigger_status,rp.due_date,
        COALESCE((SELECT SUM(r.amount) FROM receipt_records r WHERE r.tenant_id=rp.tenant_id AND r.receivable_plan_id=rp.id AND r.record_status='VALID'),0) AS received_amount,
        COALESCE((SELECT SUM(i.amount) FROM invoice_records i WHERE i.tenant_id=rp.tenant_id AND i.receivable_plan_id=rp.id AND i.record_status='VALID'),0) AS invoiced_amount
      FROM receivable_plans rp JOIN contracts c ON c.id=rp.contract_id AND c.tenant_id=rp.tenant_id
      """;
    @Select(BASE_SELECT + " WHERE rp.tenant_id=#{tenantId} ORDER BY CASE WHEN rp.due_date IS NULL THEN 1 ELSE 0 END,rp.due_date,rp.id")
    List<ReceivableRow> findAll(Long tenantId);
    @Select(BASE_SELECT + " WHERE rp.tenant_id=#{tenantId} AND rp.id=#{id} FOR UPDATE")
    ReceivableRow findByIdForUpdate(@Param("tenantId")Long tenantId,@Param("id")Long id);
    @Select(BASE_SELECT + " WHERE rp.tenant_id=#{tenantId} AND rp.id=#{id}")
    ReceivableRow findById(@Param("tenantId")Long tenantId,@Param("id")Long id);
    @Insert("INSERT INTO receivable_plans(tenant_id,contract_id,plan_name,plan_amount,trigger_status,due_date,created_by_membership_id) VALUES(#{tenantId},#{request.contractId},#{request.planName},#{request.planAmount},#{request.triggerStatus},#{request.dueDate},#{membershipId})")
    @Options(useGeneratedKeys=true,keyProperty="row.id",keyColumn="id")
    void insertPlan(@Param("row")ReceivableRow row,@Param("tenantId")Long tenantId,@Param("membershipId")Long membershipId,@Param("request")CreateReceivableRequest request);
    @Select("SELECT COALESCE(SUM(amount),0) FROM receipt_records WHERE tenant_id=#{tenantId} AND receivable_plan_id=#{planId} AND record_status='VALID'")
    BigDecimal sumReceipts(@Param("tenantId")Long tenantId,@Param("planId")Long planId);
    @Select("SELECT COALESCE(SUM(amount),0) FROM invoice_records WHERE tenant_id=#{tenantId} AND receivable_plan_id=#{planId} AND record_status='VALID'")
    BigDecimal sumInvoices(@Param("tenantId")Long tenantId,@Param("planId")Long planId);
    @Select("SELECT EXISTS(SELECT 1 FROM receipt_records WHERE tenant_id=#{tenantId} AND request_key=#{key})") boolean receiptRequestExists(@Param("tenantId")Long tenantId,@Param("key")String key);
    @Select("SELECT EXISTS(SELECT 1 FROM receipt_records WHERE tenant_id=#{tenantId} AND reference_no=#{referenceNo} AND record_status='VALID')") boolean receiptReferenceExists(@Param("tenantId")Long tenantId,@Param("referenceNo")String referenceNo);
    @Insert("INSERT INTO receipt_records(tenant_id,receivable_plan_id,amount,receipt_date,payment_method,payer_name,reference_no,note,record_status,request_key,created_by_membership_id) VALUES(#{tenantId},#{planId},#{request.amount},#{request.receiptDate},#{request.paymentMethod},#{request.payerName},#{request.referenceNo},#{request.note},'VALID',#{request.requestKey},#{membershipId})")
    void insertReceipt(@Param("tenantId")Long tenantId,@Param("planId")Long planId,@Param("membershipId")Long membershipId,@Param("request")CreateReceiptRequest request);
    @Select("SELECT EXISTS(SELECT 1 FROM invoice_records WHERE tenant_id=#{tenantId} AND request_key=#{key})") boolean invoiceRequestExists(@Param("tenantId")Long tenantId,@Param("key")String key);
    @Select("SELECT EXISTS(SELECT 1 FROM invoice_records WHERE tenant_id=#{tenantId} AND invoice_no=#{invoiceNo} AND record_status='VALID')") boolean invoiceNoExists(@Param("tenantId")Long tenantId,@Param("invoiceNo")String invoiceNo);
    @Insert("INSERT INTO invoice_records(tenant_id,receivable_plan_id,invoice_no,amount,invoice_date,invoice_type,invoice_title,record_status,request_key,created_by_membership_id) VALUES(#{tenantId},#{planId},#{request.invoiceNo},#{request.amount},#{request.invoiceDate},#{request.invoiceType},#{request.invoiceTitle},'VALID',#{request.requestKey},#{membershipId})")
    void insertInvoice(@Param("tenantId")Long tenantId,@Param("planId")Long planId,@Param("membershipId")Long membershipId,@Param("request")CreateInvoiceRequest request);
}

