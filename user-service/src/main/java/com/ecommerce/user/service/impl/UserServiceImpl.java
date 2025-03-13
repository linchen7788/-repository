package com.ecommerce.user.service.impl;

import com.ecommerce.common.entity.UserDO;
import com.ecommerce.user.mapper.UserMapper;
import com.ecommerce.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public Boolean register(UserDO user) {
        UserDO existUser = userMapper.selectByUsername(user.getUsername());
        if (existUser != null) {
            return false;
        }
        return userMapper.insert(user) > 0;
    }

    @Override
    public Boolean login(UserDO user) {
        UserDO dbUser = userMapper.selectByUsername(user.getUsername());
        return dbUser != null && dbUser.getPassword().equals(user.getPassword());
    }

    @Override
    public UserDO getById(Long id) {
        return userMapper.selectById(id);
    }
}