package com.csye6225spring2020.controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.csye6225spring2020.entity.Bill;
import com.csye6225spring2020.entity.User;
import com.csye6225spring2020.repository.BillRepository;
import com.csye6225spring2020.repository.FileRepository;
import com.csye6225spring2020.repository.UserRepositry;
import com.csye6225spring2020.util.S3Util;
import com.google.gson.Gson;
import com.timgroup.statsd.StatsDClient;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@RestController
@Profile("aws")
public class FileS3Controller {

    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    BillRepository billRepositry;
    @Autowired
    UserRepositry userRepositry;
    @Autowired
    FileRepository fileRepositry;
    @Autowired
    Environment env;

    @Autowired
    private StatsDClient stats;

    private final static Logger logger = LoggerFactory.getLogger(FileS3Controller.class);



    @PostMapping(path = "v1/bill/{id}/file", produces = "application/json", consumes = "multipart/form-data")
    @ResponseBody
    public ResponseEntity<String> addFile(@RequestHeader HttpHeaders head, @PathVariable(value = "id") UUID billid, @RequestParam("file") MultipartFile f) throws IOException, NoSuchAlgorithmException {
        stats.incrementCounter("endpoint.myaddFileToS3.http.post");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepositry.findByEmailAddress(login[0]);
        if (u == null) {
            stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (f == null) {
            stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please attach a file");
        }

        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepositry.findById(billid);
            if (b == null) {
                stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            }
            if (!b.getOwnerid().equals(u.getId())) {
                stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            }
            com.csye6225spring2020.entity.File file = fileRepositry.findByBillId(billid);
            if (file != null) {
                stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Files already exists.Delete it before you rewrite");
            }
            if (f.getOriginalFilename().endsWith(".pdf") || f.getOriginalFilename().endsWith(".png") || f.getOriginalFilename().endsWith(".jpg") || f.getOriginalFilename().endsWith(".jpeg")) {

                String filename = f.getOriginalFilename();
                InputStream is = f.getInputStream();
                String suffix = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
                String newFileName = System.currentTimeMillis() + "." + suffix;
                AmazonS3 s3 = AmazonS3ClientBuilder.standard().build();
                try {
                    s3.putObject(new PutObjectRequest(env.getProperty("bucket.name"), newFileName, is, new ObjectMetadata()).withCannedAcl(CannedAccessControlList.Private));
                    String url = S3Util.productRetrieveFileFromS3("", newFileName, env.getProperty("bucket.name"));
                    logger.info("Getting my s3:Url after storing\t:",url);
                    String etag = "";
                    AmazonS3 s3client = AmazonS3ClientBuilder.standard().build();
                    for (S3ObjectSummary summary : S3Objects.inBucket(s3client, env.getProperty("bucket.name"))) {
                        etag=summary.getETag();
                    }
                    // Storing meta data in the DB: MSQL
                    com.csye6225spring2020.entity.File file1 = new com.csye6225spring2020.entity.File();
                    file1.setBillId(billid);
                    file1.setFileName(f.getOriginalFilename());
                    file1.setUpload_date(new Date().toString());
                    file1.setUrl(url);
                    file1.setMd5hash(etag);
                    file1.setMimeType(f.getContentType().toString());
                    file1.setSize(f.getSize());
                    b.setAttachment(file1);
                    b.setUpdated_ts(new Date().toString());

                    fileRepositry.save(file1);
                    Gson gson = new Gson();
                    String json = gson.toJson(file1);
                    logger.info("Added file to S3 Sucessfully");
                    stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.OK).body(json);
                } catch (AmazonServiceException e) {
                    logger.error("Amazon Service Exception :",e.getErrorMessage());
                    stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getErrorMessage());
                }
            } else {
                stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Wrong file format");
            }
        }
        stats.recordExecutionTime("myaddFileS3Latency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }


    @GetMapping(path = "v1/bill/{bid}/file/{fid}", produces = "application/json")
    public ResponseEntity<String> getFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) {
        stats.incrementCounter("endpoint.mygetFileFromS3.http.get");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepositry.findByEmailAddress(login[0]);
        if (u == null) {
            stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepositry.findById(billid);
            if (b == null) {
                stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            }
            if (!b.getOwnerid().equals(u.getId())) {
                stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            }
            else {

                com.csye6225spring2020.entity.File fi = fileRepositry.findById(fileid);
                if (fi == null) {
                    stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("NO such file exists");
                }
                if (fi.getBillId().equals(billid)) {
                    Gson gson = new Gson();
                    String json = gson.toJson(fi);
                    logger.info("File retrieved from S3 Sucessfully");
                    stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.OK).body(json);
                } else {
                    stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("This billid doesnt have any files");
                }
            }
        }
        stats.recordExecutionTime("mygetFileS3Latency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");

    }

    @DeleteMapping(path = "v1/bill/{bid}/file/{fid}")
    public ResponseEntity<String> deleteFile(@RequestHeader HttpHeaders head, @PathVariable(value = "bid") UUID billid, @PathVariable(value = "fid") UUID fileid) throws IOException {
        stats.incrementCounter("endpoint.mydeleteFileFromS3.http.delete");
        long startTime = System.currentTimeMillis();
        byte decoded[] = Base64.getDecoder().decode(head.getFirst("authorization").substring(6));
        String decodedstring = new String(decoded);
        String login[] = decodedstring.split(":");
        User u = userRepositry.findByEmailAddress(login[0]);
        if (u == null) {
            stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Not in database!!!");
        }
        if (BCrypt.checkpw(login[1], u.getPassword())) {
            Bill b = billRepositry.findById(billid);
            if (b == null) {
                stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not FOUND!!!");
            }
            if (!b.getOwnerid().equals(u.getId())) {
                stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("UNAUTHORIZED!!!");
            }
            else {
                com.csye6225spring2020.entity.File fi = fileRepositry.findById(fileid);
                if (fi == null) {
                    stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No such file exists");
                }
                if (fi.getBillId().equals(billid)) {
                    String[] value = fi.getUrl().split("/" + env.getProperty("bucket.name"));
                    String[] keyValue = value[1].split("/");
                    AmazonS3 s3client = AmazonS3ClientBuilder.standard().build();
                    String toDelete = "";
                    for (S3ObjectSummary summary : S3Objects.inBucket(s3client, env.getProperty("bucket.name"))) {
                        String imageName = summary.getKey();
                        if (imageName.equals(keyValue[1])) {
                            toDelete = imageName;
                            b.setAttachment(null);
                            fileRepositry.delete(fi);
                            break;
                        }
                    }
                    if (!toDelete.equals("")) {
                        s3client.deleteObject(env.getProperty("bucket.name"), toDelete);
                    }
                    logger.info("File deleted from S3 Sucessfully");
                    stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
                    return ResponseEntity.status(HttpStatus.OK).body("File deleted successfully");
                }
            }
        }
        stats.recordExecutionTime("mydeleteFileS3Latency", System.currentTimeMillis() - startTime);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Please use correct login details!!!");
    }

}
