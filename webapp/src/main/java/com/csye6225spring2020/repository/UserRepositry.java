package com.csye6225spring2020.repository;


import com.csye6225spring2020.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

public interface UserRepositry extends JpaRepository<User, Long> {


    User findByEmailAddress(String emailaddress);


}
