package com.dailycodework.lakeSidehotel.service;

import com.dailycodework.lakeSidehotel.model.User;

import java.util.List;

public interface IUserService {

    User registerUser(User user);
    List<User> getUsers();
    void deleteUser(String email);
    User getUser(String email);



}
