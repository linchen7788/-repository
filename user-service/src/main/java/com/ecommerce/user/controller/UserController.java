package com.ecommerce.user.controller;

import com.ecommerce.common.entity.UserDO;
import com.ecommerce.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Boolean> register(@RequestBody UserDO user) {
        boolean result = userService.register(user);
        return result ? ResponseEntity.ok(true) : ResponseEntity.status(HttpStatus.BAD_REQUEST).body(false);
    }

    @PostMapping("/login")
    public ResponseEntity<Boolean> login(@RequestBody UserDO user) {
        Boolean result = userService.login(user);
        return result ? ResponseEntity.ok(true) : ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDO> getUser(@PathVariable Long id) {
        UserDO user = userService.getById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}