package com.csye6225spring2020.controller;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.csye6225spring2020.awsmetrics.SQSConfig;
import com.csye6225spring2020.entity.Bill;
import com.csye6225spring2020.entity.PaymentStatus;
import com.csye6225spring2020.entity.User;
import com.csye6225spring2020.model.BillSQSPojo;
import com.csye6225spring2020.repository.BillRepository;
import com.csye6225spring2020.repository.FileRepository;
import com.csye6225spring2020.repository.UserRepositry;
import com.csye6225spring2020.thread.MySQSPolling;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;
import org.apache.logging.log4j.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.transaction.Transactional;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;

@RestController
@Transactional
public class BillController {


    @Autowired
    BillRepository billRepository;

    @Autowired
    Environment environment;

    @Autowired(required=false)
    FileController fileController;

    @Autowired(required=false)
    FileS3Controller fileS3Controller;

    @Autowired
    UserRepositry userRepositry;

    @Autowired
    FileRepository fileRepository;

    @Autowired(required = false)
    SQSConfig sqs;

    @Autowired
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(BillController.class);


    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    @PostMapping(path = "/v1/bill", consumes = "application/json", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> createBill(@RequestHeader HttpHeaders headers, @RequestBody Bill bill) throws ParseException {
        stats.incrementCounter("endpoint.mypostBill.http.post");
        long startTime = System.currentTimeMillis();
        if (!Strings.isBlank(bill.getVendor()) && !Strings.isBlank(bill.getBill_date()) && !Strings.isBlank(bill.getDue_date())
                && bill.getAmount_due() > 0 && bill.getCategories().size() > 0 && bill.getPaymentStatus() != null) {

           if(bill.getAmount_due() > 0.01) {
               byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
               String decodedToken = new String(actualByte);
               String[] credentials = decodedToken.split(":");
               User userObj = userRepositry.findByEmailAddress(credentials[0]);

               if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                   bill.setCreated_ts(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                   bill.setUpdated_ts(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                   bill.setOwnerid(userObj.getId());
                   if (bill.getPaymentStatus().equals("paid")) {
                       bill.setPaymentStatus(PaymentStatus.paid);
                   } else if (bill.getPaymentStatus().equals("due")) {
                       bill.setPaymentStatus(PaymentStatus.due);
                   } else if (bill.getPaymentStatus().equals("paid")) {
                       bill.setPaymentStatus(PaymentStatus.paid);
                   } else if (bill.getPaymentStatus().equals("no_payment_required")) {
                       bill.setPaymentStatus(PaymentStatus.no_payment_required);
                   }
                   formatter.parse(bill.getBill_date());
                   formatter.parse(bill.getDue_date());
                   Set <String> set = new HashSet<>();
                   for(String s : bill.getCategories()){
                       if(!set.add(s)){
                           stats.recordExecutionTime("mycreateBillLatency", System.currentTimeMillis() - startTime);
                           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categories should be unique");
                       }
                   }
                   billRepository.save(bill);
                   bill.setId(bill.getId());
                   JSONObject jsonObject = new JSONObject(bill);
                   jsonObject.accumulate("attachment",new JSONObject());
                   logger.info("Bill created successfully\t"+bill.getId());
                   stats.recordExecutionTime("mycreateBillLatency", System.currentTimeMillis() - startTime);
                   return ResponseEntity.status(HttpStatus.CREATED).body(jsonObject.toString());
               } else {
                   logger.warn("Invalid Credentials");
                   stats.recordExecutionTime("mycreateBillLatency", System.currentTimeMillis() - startTime);
                   return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid crendentials");
               }
           }else{
               stats.recordExecutionTime("mycreateBillLatency", System.currentTimeMillis() - startTime);
               return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount due should be greater than 0.01 $'s");
           }
        } else {
            stats.recordExecutionTime("mycreateBillLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All fields are required.");
        }

    }



    @GetMapping(path = "/v1/bills", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getBill(@RequestHeader HttpHeaders headers) {
        stats.incrementCounter("endpoint.mygetBillsv1.http.get");
        long startTime = System.currentTimeMillis();
        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        if (!Strings.isBlank(decodedToken)) {
            String[] credentials = decodedToken.split(":");
            User userObj = userRepositry.findByEmailAddress(credentials[0]);
            JSONArray addbillsJson = new JSONArray();
            if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                List<Bill> billsList = billRepository.findByOwnerid(userObj.getId());
                JSONArray billtojsonArray = new JSONArray(billsList);
                for (int i = 0; i < billsList.size(); i++) {
                    JSONObject getJSONObject = billtojsonArray.getJSONObject(i);
                    if (!getJSONObject.has("attachment")) {
                        getJSONObject.accumulate("attachment", new JSONObject());
                    }
                    addbillsJson.put(getJSONObject);
                }
                logger.info("Bill retrived successfully\t");
                stats.recordExecutionTime("mygetBillsV1Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.OK).body(addbillsJson.toString());
            } else {
                stats.recordExecutionTime("mygetBillsV1Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credentials");
            }
        }else{
            stats.recordExecutionTime("mygetBillsV1Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide valid user name and passeord to view bills");
        }


    }

    @GetMapping(path = "/v2/bills", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getBills1(@RequestHeader HttpHeaders headers) {
        stats.incrementCounter("endpoint.mygetAllBillsv2.http.get");
        long startTime = System.currentTimeMillis();
        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        if (!Strings.isBlank(decodedToken)) {
            String[] credentials = decodedToken.split(":");
            User userObj = userRepositry.findByEmailAddress(credentials[0]);
            JSONArray addbillsJson = new JSONArray();
            if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                List<Bill> billsList = billRepository.findByOwnerid(userObj.getId());
                JSONArray billtojsonArray = new JSONArray(billsList);
                for (int i = 0; i < billsList.size(); i++) {
                    JSONObject getJSONObject = billtojsonArray.getJSONObject(i);
                    if (!getJSONObject.has("attachment")) {
                        getJSONObject.accumulate("attachment", new JSONObject());
                    }
                    addbillsJson.put(getJSONObject);
                }
                logger.info("Bill retrived for end point V2 successfully\t");
                stats.recordExecutionTime("mygetBillsV2Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.OK).body(addbillsJson.toString());
            } else {
                stats.recordExecutionTime("mygetBillsV2Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credetials");
            }
        }else{
            stats.recordExecutionTime("mygetBillsV2Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide valid user name and passeord to view bills");
        }


    }



    @GetMapping(path = "/v1/bill/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> getBillById(@RequestHeader HttpHeaders headers,@PathVariable(value="id") UUID billid) {
        stats.incrementCounter("endpoint.mygetBillById.http.get");
        long startTime = System.currentTimeMillis();
        if (headers.size()>0) {
            byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
            String decodedToken = new String(actualByte);
            String[] credentials = decodedToken.split(":");
            User userObj = userRepositry.findByEmailAddress(credentials[0]);

            if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                Bill billsByiD = billRepository.findById(billid);

                if(billsByiD != null && billsByiD.getOwnerid().equals(userObj.getId())){
                    JSONObject jsonObject = new JSONObject(billsByiD);
                    if(!jsonObject.has("attachment")) {
                        jsonObject.accumulate("attachment", new JSONObject());
                    }
                    stats.recordExecutionTime("mygetBillByIdlatency", System.currentTimeMillis() - startTime);
                    logger.info("Bill by id retrieved successfully\t");
                    return ResponseEntity.status(HttpStatus.OK).body(jsonObject.toString());
                }else{
                    stats.recordExecutionTime("mygetBillByIdlatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can view only your bills");
                }
            } else {
                stats.recordExecutionTime("mygetBillByIdlatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credetials");
            }
        }else{
            stats.recordExecutionTime("mygetBillByIdlatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide valid user name and passeord to view bills");
        }


    }

    @DeleteMapping(path = "/v1/bill/{id}")
    @ResponseBody
    public ResponseEntity<String> deleteBillById(@RequestHeader HttpHeaders headers,@PathVariable(value="id") UUID billid) throws IOException {
        stats.incrementCounter("endpoint.mydeleteBillById.http.delete");
        long startTime = System.currentTimeMillis();
        if (headers.size()>0) {
            byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
            String decodedToken = new String(actualByte);
            String[] credentials = decodedToken.split(":");
            User userObj = userRepositry.findByEmailAddress(credentials[0]);

            if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                Bill billsByiD = billRepository.findById(billid);
                String [] env = environment.getActiveProfiles();
                List<String> list = Arrays.asList(env);
                if(list.contains("local") && fileRepository.findByBillId(billid)!=null) {
                    fileController.deleteFile(headers,billid,billsByiD.getAttachment().getId());
                }
                if( list.contains("aws")&&fileRepository.findByBillId(billid)!=null) {
                    fileS3Controller.deleteFile(headers,billid,billsByiD.getAttachment().getId());
                }
                if(billsByiD != null && billsByiD.getOwnerid().equals(userObj.getId())) {
                    Long billDelNo = billRepository.deleteById(billid);
                    logger.info("Deleted Bill No\t" +billDelNo);
                    if (billDelNo == 1) {
                        logger.info("Bill Deleted successfully\t");
                        stats.recordExecutionTime("mydeleteBillByIdLatency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("Deleted Bill");
                    } else {
                        logger.warn("Cannot Delete Bill by above Id\t");
                        stats.recordExecutionTime("mydeleteBillByIdLatency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot Delete Bill by above Id");
                    }
                }else{
                    stats.recordExecutionTime("mydeleteBillByIdLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can delete only your bills");
                }
            } else {
                stats.recordExecutionTime("mydeleteBillByIdLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Credetials");
            }
        }else{
            stats.recordExecutionTime("mydeleteBillByIdLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please provide valid user name and passeord to view bills");
        }


    }


    @PutMapping(path = "/v1/bill/{id}", produces = "application/json")
    @ResponseBody
    public ResponseEntity<String> udateBillById(@RequestHeader HttpHeaders headers,@RequestBody Bill bill, @PathVariable(value="id") UUID billid) {
        stats.incrementCounter("endpoint.myupdateBillById.http.put");
        long startTime = System.currentTimeMillis();
        if (headers.size()>0 && !Strings.isBlank(bill.getVendor()) && !Strings.isBlank(bill.getBill_date()) && !Strings.isBlank(bill.getDue_date())
                && bill.getAmount_due() > 0 && bill.getCategories().size() > 0 && bill.getPaymentStatus() != null) {

            if(bill.getAmount_due() > 0.01) {
            byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
            String decodedToken = new String(actualByte);
            String[] credentials = decodedToken.split(":");
            User userObj = userRepositry.findByEmailAddress(credentials[0]);
            if (userObj != null && BCrypt.checkpw(credentials[1], userObj.getPassword())) {
                Bill billsByiD = billRepository.findById(billid);

                if(billsByiD != null) {
                    if (userObj.getId().equals(billsByiD.getOwnerid())) {
                        billsByiD.setVendor(bill.getVendor());
                        billsByiD.setBill_date(bill.getBill_date());
                        billsByiD.setDue_date(bill.getDue_date());
                        billsByiD.setAmount_due(bill.getAmount_due());
                        billsByiD.setCategories(bill.getCategories());
                        billsByiD.setUpdated_ts(new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(new Date()));
                        if (bill.getPaymentStatus().equals("paid")) {
                            billsByiD.setPaymentStatus(PaymentStatus.paid);
                        } else if (bill.getPaymentStatus().equals("due")) {
                            billsByiD.setPaymentStatus(PaymentStatus.due);
                        } else if (bill.getPaymentStatus().equals("paid")) {
                            billsByiD.setPaymentStatus(PaymentStatus.paid);
                        } else if (bill.getPaymentStatus().equals("no_payment_required")) {
                            billsByiD.setPaymentStatus(PaymentStatus.no_payment_required);
                        }
                        Set <String> set = new HashSet<>();
                        for(String s : bill.getCategories()){
                            if(!set.add(s)){
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Categories should be unique");
                            }
                        }
                        billRepository.save(billsByiD);
                        Gson gson = new Gson();
                        String json = gson.toJson(billsByiD);
                        logger.info("Bill updated successfully");
                        stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.OK).body(json);
                    } else {
                        logger.info("You can update only your bills");
                        stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("You can update only your bills");
                    }
                }else{
                    stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Cannot Update Bill by above Id");
                }


            } else {
                stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Please provide username and password");
            }
            }else{
                stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Amount due should be greater than 0.01 $'s");
            }
        }else{
            stats.recordExecutionTime("myupdateBillByIdLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("All fields are required for updation");
        }


    }


    @GetMapping(path = "/v1/bills/due/{x}" ,produces = "application/json")
    public ResponseEntity<String> getBills(@RequestHeader HttpHeaders headers, @PathVariable(value="x") int x) {
        if (environment.getActiveProfiles().equals("aws"))
            stats.incrementCounter("endpoint.getBills.due.http.get");
        long startTime = System.currentTimeMillis();
        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepositry.findByEmailAddress(credentials[0]);
        if(user==null) {
            logger.warn("user doesn't exist in the database",logger.getClass());
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unauthorized user request failed");
        }
        logger.error("inside bills {x} success");
        if(x<0){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("X Should be greater than zero");
        }
        try {
            int a = (int) x;
        }catch (Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("X Should be Integer");
        }

        JSONArray addbilljsonArray = new JSONArray();
        
        if (BCrypt.checkpw(credentials[1], user.getPassword())) {
            List<Bill> bill = billRepository.findByOwnerid(user.getId());
            List<Bill>bills =new ArrayList<>();
            List<UUID> listofduebills = new ArrayList<>();
            JSONArray billtojsonArray = new JSONArray(bill);
            for (int i = 0; i < bill.size(); i++) {
                JSONObject getJSONObject = billtojsonArray.getJSONObject(i);
                String billDueDate= bill.get(i).getDue_date();
                Date todayDate = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
                String d1 =formatter.format(todayDate);
                DateTime dt1 = new DateTime(d1);
                DateTime dt2 = new DateTime(billDueDate);
                Days d =  Days.daysBetween(dt1,dt2);
                int duetime =d.getDays();
                System.out.println(duetime);
                if(duetime<=x&&duetime>=0){
                    bills.add(bill.get(i));
                    listofduebills.add(bill.get(i).getId());
                }
            }
            BillSQSPojo billSQSPojo = new BillSQSPojo();
            billSQSPojo.setEmail(user.getEmailAddress());
            billSQSPojo.setDueBillIds(listofduebills);
            emailQueueBills(billSQSPojo);
            JSONArray billtojso = new JSONArray(bills);
            logger.info("bills due returned",logger.getClass());
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.OK).body("Email Will be Sent");
        }else{
            logger.warn("unauthorized user tried to access",logger.getClass());
            stats.recordExecutionTime("getBillsLatency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credentials are wrong");
        }

    }

    public  ResponseEntity<String> emailQueueBills(BillSQSPojo billSQSPojo){
        String queueUrl = sqs.sqsClient().getQueueUrl("BillQueue").getQueueUrl();
        SendMessageRequest send_msg_request = new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageBody(new Gson().toJson(billSQSPojo))
                .withDelaySeconds(0);
        sqs.sqsClient().sendMessage(send_msg_request);
        logger.info("message is set");
        AmazonSQS sqs1 = AmazonSQSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain()).build();
        MySQSPolling mySQSPolling = new MySQSPolling(sqs1,queueUrl);
        mySQSPolling.start();
        return null;
    }
}
