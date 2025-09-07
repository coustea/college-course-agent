package com.ccut.mapper;

import com.ccut.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    int insertUser(User user);
    User getUserByUsername(String username);
    User getById(Long id);
}
