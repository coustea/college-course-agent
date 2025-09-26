package com.ccut.service;

import com.ccut.entity.User;

import java.util.List;

public interface UserService {

    int insert(User user);
    int deleteAll();
    User getByUsername(String username);
}
