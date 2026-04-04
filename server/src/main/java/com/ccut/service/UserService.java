package com.ccut.service;

import com.ccut.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    int insert(User user);
    int updateUser(User user);
    int deleteAll();
    User getByUsername(String username);
    String importStudentsFromExcel(MultipartFile file);

}
