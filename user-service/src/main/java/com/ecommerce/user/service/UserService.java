package com.ecommerce.user.service;

import com.ecommerce.common.entity.UserDO;

public interface UserService {
    Boolean register(UserDO user);
    Boolean login(UserDO user);
    UserDO getById(Long id);
}
