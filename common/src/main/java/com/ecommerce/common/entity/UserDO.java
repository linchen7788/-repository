package com.ecommerce.common.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;

@TableName("user_new")
public class UserDO {
    @TableId
    private Long id;
    
    @TableField("username")
    private String username;
    
    @TableField("password")
    private String password;
    
    @TableField("phone")
    private String phone;
    
    @TableField("status")
    private Integer status;

    public UserDO() {}

    public UserDO(Long id, String username, String password, String phone, Integer status) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.phone = phone;
        this.status = status;
    }

    // Getter和Setter方法
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    @Override
    public String toString() {
        return "UserDO{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", phone='" + phone + '\'' +
                ", status=" + status +
                '}';
    }
}