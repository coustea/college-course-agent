package com.ccut.mapper;

import com.ccut.entity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapper {

    int insertUser(User user);
    User getUserByUsername(String username);
    User getById(Long id);
    int deleteAll();
}
