package com.contractguard.platform.organization;
import java.util.List;
public record OrganizationMemberResponse(Long membershipId,String username,String displayName,String status,List<String> roleCodes){static OrganizationMemberResponse from(OrganizationMemberRow r){return new OrganizationMemberResponse(r.getMembershipId(),r.getUsername(),r.getDisplayName(),r.getMembershipStatus(),r.getRoleCodes()==null?List.of():List.of(r.getRoleCodes().split(",")));}}

