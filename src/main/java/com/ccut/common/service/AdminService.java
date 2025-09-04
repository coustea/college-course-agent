package com.ccut.common.service;

import com.ccut.common.entity.Admin;
import com.ccut.common.entity.User;
import com.ccut.common.exception.CustomerException;
import com.ccut.common.mapper.AdminMapper;
import com.ccut.common.Utils.TakenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminService {

    @Autowired
    AdminMapper adminMapper;

    //管理员登陆
    public Admin login(User user) {
        Admin dbAdmin = adminMapper.selectByUsername(user.getUsername());
        if(dbAdmin == null){
            throw new CustomerException("账号不存在！");
        }

        if(!dbAdmin.getPassword().equals(user.getPassword())){
            throw new CustomerException("账号或密码错误");
        }

        String token = TakenUtils.createToken(dbAdmin.getId() + "-" + "ADMIN", dbAdmin.getPassword());
        dbAdmin.setToken(token);
        // 写回数据库，便于后续查询也能拿到 token
        adminMapper.updateById(dbAdmin);
        return dbAdmin;
    }

    public Admin selectByName(String username){
        return adminMapper.selectByUsername(username);
    }
    
    //添加管理员
    public int addAdmin(Admin admin) {

        return adminMapper.insert(admin);
    }
    //删除管理员
    public int deleteAdmin(Integer id) {

        return adminMapper.deleteById(id);
    }
    //修改学生
    public int updateAdmin(Admin admin) {
        return adminMapper.updateById(admin);

    }
    public List<Admin> selectAll(Admin admin) {
        return adminMapper.selectAll(admin);
    }
}
