package com.ecommerce.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.common.entity.UserDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper extends BaseMapper<UserDO> {

@Select("SELECT * FROM user WHERE username = #{username}")
UserDO selectByUsername(@Param("username") String username);
}