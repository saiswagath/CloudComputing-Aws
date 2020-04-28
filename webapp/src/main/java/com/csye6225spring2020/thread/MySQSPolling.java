package com.csye6225spring2020.thread;
import java.util.Collections;
import java.util.List;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.csye6225spring2020.controller.BillController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;


import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.CreateTopicResult;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;



public class MySQSPolling extends Thread {

    AmazonSQS sqs;
    String queueUrl;

    private static final Logger logger = LoggerFactory.getLogger(MySQSPolling.class);

    public MySQSPolling(AmazonSQS sqs, String queueUrl) {
        // TODO Auto-generated constructor stub
        this.sqs = sqs;
        this.queueUrl = queueUrl;

    }

    public void run() {
        while(true) {
            
            logger.info("Polling started...");
            final ReceiveMessageRequest receive_request =new ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withWaitTimeSeconds(20);

            List<Message> result =  Collections.synchronizedList(sqs.receiveMessage(receive_request).getMessages());
            for (Message message : result) {
                logger.info("INvoking SNS...");
                logger.info("Printing message in queue\t"+message.getBody());
                AmazonSNS sns = AmazonSNSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
                sns.publish(new PublishRequest(sns.createTopic("bill_due_topic").getTopicArn(), message.getBody() ));
                logger.info("INvoKED SNS...");
                sqs.deleteMessage(queueUrl, message.getReceiptHandle());
                logger.info("Deleted record from queue...");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}