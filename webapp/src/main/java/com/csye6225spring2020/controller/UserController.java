package com.csye6225spring2020.controller;


import com.csye6225spring2020.entity.User;
import com.csye6225spring2020.repository.UserRepositry;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;
import org.apache.logging.log4j.util.Strings;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
public class UserController {
    @Autowired
    UserRepositry userRepositry;

    @Autowired
    Environment env;

    @Autowired(required = false)
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    private static String regex = "^(.+)@(.+)$";
    //Minimum eight characters, at least one letter and one number:
    private static String PASS_REGEX = "^(?=.{8,})(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$";
    Pattern passPattern = Pattern.compile(PASS_REGEX);
    Pattern pattern = Pattern.compile(regex);

    @PostMapping(path = "/v1/user", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> createUser(@RequestBody User user) {
        if (env.getActiveProfiles().equals("aws"))
        stats.incrementCounter("endpoint.mycreateuser.http.post");
        long startTime = System.currentTimeMillis();
        if (!Strings.isBlank(user.getEmailAddress()) && !Strings.isBlank(user.getFirst_name()) && !Strings.isBlank(user.getLast_name()) && !Strings.isBlank(user.getPassword())) {
            if (userRepositry.findByEmailAddress(user.getEmailAddress()) != null) {
                stats.recordExecutionTime("mycreateUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Account already exists with this given emailid");
            }

            Matcher matcher = pattern.matcher(user.getEmailAddress());
            Matcher passMatcher = passPattern.matcher(user.getPassword());

            if (!matcher.matches()) {
                stats.recordExecutionTime("mycreateUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect email format");
            } else if (!passMatcher.matches()) {
                stats.recordExecutionTime("mycreateUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Minimum password should be of 8 characters length and contain one upper case, one lower case and special character");
            } else {
                user.setAccount_created(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                user.setAccount_updated(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12)));
                userRepositry.save(user);
                User userObj = userRepositry.findByEmailAddress(user.getEmailAddress());
                User tempUser = new User();
                tempUser.setId(userObj.getId());
                tempUser.setFirst_name(userObj.getFirst_name());
                tempUser.setLast_name(userObj.getLast_name());
                tempUser.setEmailAddress(userObj.getEmailAddress());
                tempUser.setAccount_created(userObj.getAccount_created());
                tempUser.setAccount_updated(userObj.getAccount_updated());
                Gson gson = new Gson();
                String json = gson.toJson(tempUser);
                if (env.getActiveProfiles().equals("aws"))
                stats.recordExecutionTime("mycreateUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.CREATED).body(json);
            }
        } else {
            stats.recordExecutionTime("mycreateUserLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating user ");
        }

    }


    @PutMapping(path = "/v1/user/self", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> updateUser(@RequestHeader HttpHeaders headers, @RequestBody User user) {
        stats.incrementCounter("endpoint.myupdateUser.http.put");
        long startTime = System.currentTimeMillis();
        if (user.getAccount_created() != null || user.getAccount_updated() != null) {
            stats.recordExecutionTime("myupdateUserLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("account created or account updated fileds cannot be modifies ");
        } else {
            byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
            String decodedToken = new String(actualByte);
            String[] credentials = decodedToken.split(":");
            Matcher passMatcher = passPattern.matcher(user.getPassword());
            User userObj = userRepositry.findByEmailAddress(credentials[0]);
            if(userObj != null) {
                if (BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                    if (user.getFirst_name() != null && user.getLast_name() != null && user.getPassword() != null && passMatcher.matches() && user.getEmailAddress() != null && credentials[0].equals(user.getEmailAddress())) {
                        userObj.setFirst_name(user.getFirst_name());

                        userObj.setLast_name(user.getLast_name());

                        userObj.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt(12)));

                        userObj.setAccount_updated(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                    } else {
                        stats.recordExecutionTime("myupdateUserLatency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide all fields for updation with password restrictions and email is a mandatory field in request but it cannot be updated ");
                    }

                    userRepositry.save(userObj);
                    stats.recordExecutionTime("myupdateUserLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body("UPDATED ACCOUNT ");

                } else {
                    logger.warn("Authorization failed");
                    stats.recordExecutionTime("myupdateUserLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use valid credentials ");
                }
            }else{
                logger.warn("Authorization failed");
                stats.recordExecutionTime("myupdateUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use valid email address and password ");
            }
        }


    }

    @GetMapping(path = "/v1/user/self", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getUser(@RequestHeader HttpHeaders headers) {
        stats.incrementCounter("endpoint.mygetUser.http.get");
        long startTime = System.currentTimeMillis();
        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepositry.findByEmailAddress(credentials[0]);
        if(user != null) {
            if (BCrypt.checkpw(credentials[1], user.getPassword())) {
                User tempUser = new User();
                tempUser.setFirst_name(user.getFirst_name());
                tempUser.setLast_name(user.getLast_name());
                tempUser.setEmailAddress(user.getEmailAddress());
                tempUser.setAccount_created(user.getAccount_created());
                tempUser.setAccount_updated(user.getAccount_updated());
                Gson gson = new Gson();
                String json = gson.toJson(tempUser);
                logger.info("Fetched user with email id\t"+tempUser.getEmailAddress());
                stats.recordExecutionTime("mygetUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.OK).body(json);
            } else {
                logger.warn("Authorization failed");
                stats.recordExecutionTime("mygetUserLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use valid credentials ");
            }
        }else{
            logger.warn("Authorization failed");
            stats.recordExecutionTime("mygetUserLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use valid email address and password ");
        }


    }

}
