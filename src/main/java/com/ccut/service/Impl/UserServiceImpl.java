package com.ccut.service.Impl;

import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;


    @Override
    public int insert(User user) {
        return userMapper.insertUser(user);
    }

    @Override
    public int deleteAll() {
        return userMapper.deleteAll();
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.getUserByUsername(username);
    }
}
