package com.ccut.service.Impl;

import com.ccut.entity.User;
import com.ccut.mapper.UserMapper;
import com.ccut.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserMapper userMapper;


    @Override
    public int insert(User user) {
        log.debug("执行方法：insert, 参数：user={}", user != null ? user.getUsername() : "null");
        try {
            int result = userMapper.insertUser(user);
            log.info("用户插入成功：username={}, result={}", user != null ? user.getUsername() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("用户插入失败：username={}, error={}", user != null ? user.getUsername() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int updateUser(User user) {
        log.debug("执行方法：updateUser, 参数：user={}", user != null ? "id=" + user.getId() : "null");
        try {
            int result = userMapper.updateUser(user);
            log.info("用户更新成功：userId={}, result={}", user != null ? user.getId() : "null", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("用户更新失败：userId={}, error={}", user != null ? user.getId() : "null", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public int deleteAll() {
        log.debug("执行方法：deleteAll");
        try {
            int result = userMapper.deleteAll();
            log.info("删除所有用户成功：result={}", result);
            log.debug("方法返回：result={}", result);
            return result;
        } catch (Exception e) {
            log.error("删除所有用户失败：error={}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public User getByUsername(String username) {
        log.debug("执行方法：getByUsername, 参数：username={}", username);
        try {
            User result = userMapper.getUserByUsername(username);
            log.debug("方法返回：result={}", result != null ? "id=" + result.getId() : "null");
            return result;
        } catch (Exception e) {
            log.error("查询用户失败：username={}, error={}", username, e.getMessage(), e);
            throw e;
        }
    }
}
