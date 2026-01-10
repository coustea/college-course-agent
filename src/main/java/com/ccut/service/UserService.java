package com.ccut.service;

import com.ccut.entity.User;

public interface UserService {

    int insert(User user);
    int updateUser(User user);
    int deleteAll();
    User getByUsername(String username);

}
