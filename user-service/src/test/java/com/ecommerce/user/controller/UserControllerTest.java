package com.ecommerce.user.controller;

import com.ecommerce.common.entity.UserDO;
import com.ecommerce.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    private UserDO testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserDO();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
    }

    @Test
    void registerSuccess() throws Exception {
        when(userService.register(any(UserDO.class))).thenReturn(true);

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newuser\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void registerDuplicateUser() throws Exception {
        when(userService.register(any(UserDO.class))).thenReturn(false);

        mockMvc.perform(post("/user/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSuccess() throws Exception {
        when(userService.login(any(UserDO.class))).thenReturn(true);

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void loginWrongPassword() throws Exception {
        when(userService.login(any(UserDO.class))).thenReturn(false);

        mockMvc.perform(post("/user/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"testuser\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserExist() throws Exception {
        when(userService.getById(1L)).thenReturn(testUser);

        mockMvc.perform(get("/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void getUserNotExist() throws Exception {
        when(userService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/user/999"))
                .andExpect(status().isNotFound());
    }
}