package com.csye6225spring2020.controller;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @Autowired
    Environment env;

    @Autowired
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(HomeController.class);



    @RequestMapping("/")
       public String home(){

           stats.incrementCounter("endpoint.getMyHomeUrl.http.get");
           long startTime = System.currentTimeMillis();
           logger.info("******->"+env.getProperty("db.url"));
           logger.info("*****->"+env.getProperty("bucket.name"));
           stats.recordExecutionTime("myHomePageLatency", System.currentTimeMillis() - startTime);
           return "Welcome to Cloud Project CSYE 6225";
       }
}
