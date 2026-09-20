package com.contractguard.platform.notification;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface OutboxMapper {
    @Insert("""
            INSERT IGNORE INTO outbox_events(tenant_id,event_key,event_type,aggregate_type,aggregate_id,payload_json)
            VALUES(#{tenantId},#{eventKey},#{eventType},#{aggregateType},#{aggregateId},CAST(#{payloadJson} AS JSON))
            """)
    int insert(@Param("tenantId") Long tenantId, @Param("eventKey") String eventKey,
               @Param("eventType") String eventType, @Param("aggregateType") String aggregateType,
               @Param("aggregateId") Long aggregateId, @Param("payloadJson") String payloadJson);

    @Update("""
            UPDATE outbox_events SET event_status='PENDING',available_at=NOW(3),last_error='Recovered after interrupted processing'
            WHERE event_status='PROCESSING' AND available_at<DATE_SUB(NOW(3),INTERVAL 5 MINUTE)
            """)
    int recoverStale();

    @Select("""
            SELECT id,tenant_id,event_type,payload_json FROM outbox_events
            WHERE event_status IN ('PENDING','FAILED') AND attempts<5 AND available_at<=NOW(3)
            ORDER BY id LIMIT 20
            """)
    List<OutboxEventRow> findDue();

    @Update("""
            UPDATE outbox_events SET event_status='PROCESSING',attempts=attempts+1,available_at=NOW(3)
            WHERE id=#{id} AND event_status IN ('PENDING','FAILED') AND attempts<5 AND available_at<=NOW(3)
            """)
    int claim(Long id);

    @Update("UPDATE outbox_events SET event_status='PUBLISHED',processed_at=NOW(3),last_error=NULL WHERE id=#{id} AND event_status='PROCESSING'")
    int markPublished(Long id);

    @Update("""
            UPDATE outbox_events SET event_status='FAILED',last_error=#{message},
              available_at=DATE_ADD(NOW(3),INTERVAL 30 SECOND)
            WHERE id=#{id} AND event_status='PROCESSING'
            """)
    int markFailed(@Param("id") Long id, @Param("message") String message);
}

