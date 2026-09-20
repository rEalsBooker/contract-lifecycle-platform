package com.contractguard.platform.identity;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Select("""
            SELECT id, username, password_hash, display_name, status
            FROM app_users WHERE username = #{username}
            """)
    UserAccount findByUsername(String username);

    @Select("""
            SELECT id, username, password_hash, display_name, status
            FROM app_users WHERE id = #{userId}
            """)
    UserAccount findById(Long userId);
}

