package com.csye6225spring2020.controller;



import com.csye6225spring2020.entity.Bill;
import com.csye6225spring2020.entity.User;
import com.csye6225spring2020.repository.BillRepository;
import com.csye6225spring2020.repository.FileRepository;
import com.csye6225spring2020.repository.UserRepositry;
import com.google.gson.Gson;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
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
@Profile("local")
public class FileController {

    DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    @Autowired
    BillRepository billRepositry;
    @Autowired
    UserRepositry userRepositry;
    @Autowired
    FileRepository fileRepositry;

    @PostMapping(path = "v1/bill/{id}/file" ,produces="application/json",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<String> postFile(@RequestHeader HttpHeaders headers, @PathVariable(value="id") UUID billid,
                                           @RequestParam("file")MultipartFile multipartFile) throws IOException, NoSuchAlgorithmException {

        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepositry.findByEmailAddress(credentials[0]);
        if (user == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User doesn't exists in the data base");
        Bill bill =billRepositry.findById(billid);
        if (!BCrypt.checkpw(credentials[1], user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unauthorized user request failed");

        }

        if(bill==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bill doesn't exists in the data base");
        if(!bill.getOwnerid().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized user request failed");

        com.csye6225spring2020.entity.File fileFromdb = fileRepositry.findByBillId(billid);
        if(fileFromdb!=null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file is already attached to the bill please remove the existing file");
        if(multipartFile==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("please attach the file");
        if(multipartFile.isEmpty()||multipartFile.getOriginalFilename().endsWith(".pdf")||multipartFile.getOriginalFilename().endsWith(".png")||
                multipartFile.getOriginalFilename().endsWith(".jpg")||multipartFile.getOriginalFilename().endsWith(".jpeg")){
            File directory = new File("tmp");
            if(!directory.exists())
                directory.mkdirs();
            String filename = multipartFile.getOriginalFilename();
            String etxension = filename.substring(filename.lastIndexOf('.'));
            String fileOriginalName = filename.substring(0,filename.lastIndexOf('.'));
            Path path = Paths.get("tmp/"+fileOriginalName+"_"+billid+etxension);
            ArrayList<com.csye6225spring2020.entity.File> filedb= fileRepositry.findAllByFileName(filename);
            File fileToPath=null;
            if(!Files.exists(path)){
                fileToPath = new File(directory.getAbsolutePath()+File.separator+fileOriginalName+"_"+billid+etxension);
                byte[] bytes = multipartFile.getBytes();
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(fileToPath));
                bufferedOutputStream.write(bytes);
                bufferedOutputStream.close();
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                String checksum = getFileChecksum(md5Digest, fileToPath);
                String mimeType = Files.probeContentType(path);
                System.out.println("I am here ");
                com.csye6225spring2020.entity.File filetoDb = new com.csye6225spring2020.entity.File();
                filetoDb.setUpload_date(new Date().toString());
                filetoDb.setMimeType(mimeType);
                filetoDb.setBillId(billid);
                filetoDb.setUrl(fileToPath.getCanonicalPath());
                filetoDb.setFileName(filename);
                filetoDb.setSize(fileToPath.length());
                filetoDb.setMd5hash(checksum);
                bill.setAttachment(filetoDb);
                bill.setUpdated_ts(new Date().toString());
                fileRepositry.save(filetoDb);
                Gson gson = new Gson();
                String json = gson.toJson(filetoDb);
                return  ResponseEntity.status(HttpStatus.CREATED).body(json);

            }


        }
        return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body("check");
    }
    @GetMapping(path = "v1/bill/{billId}/file/{fileId}" ,produces="application/json")
    @ResponseBody
    public ResponseEntity<String> postFile(@RequestHeader HttpHeaders headers, @PathVariable(value="billId") UUID billid,
                                           @PathVariable(value="fileId") UUID fileId    )  {

        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepositry.findByEmailAddress(credentials[0]);
        if (user == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User doesn't exists in the data base");
        Bill bill =billRepositry.findById(billid);
        if (!BCrypt.checkpw(credentials[1], user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unauthorized user request failed");

        }
        if(!bill.getOwnerid().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized user request failed");
        if(bill==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bill doesn't exists in the data base");
        com.csye6225spring2020.entity.File file = fileRepositry.findById(fileId);
        if(file ==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file doesn't exists ");

        if(!file.getBillId().equals(billid)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("billid and file id doesn't match ");
        }

        JSONObject json = new JSONObject(file);
        json.remove("billId");
        return ResponseEntity.status(HttpStatus.OK).body(json.toString());

    }

    @DeleteMapping(path = "v1/bill/{billId}/file/{fileId}" ,produces="application/json")
    @ResponseBody
    public ResponseEntity<String> deleteFile(@RequestHeader HttpHeaders headers, @PathVariable(value="billId") UUID billid,
                                             @PathVariable(value="fileId") UUID fileId ) throws IOException {

        byte[] actualByte = Base64.getDecoder().decode(headers.getFirst("authorization").substring(6));
        String decodedToken = new String(actualByte);
        String[] credentials = decodedToken.split(":");
        User user = userRepositry.findByEmailAddress(credentials[0]);
        if (user == null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User doesn't exists in the data base");
        Bill bill =billRepositry.findById(billid);
        if (!BCrypt.checkpw(credentials[1], user.getPassword())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unauthorized user request failed");

        }
        if(!bill.getOwnerid().equals(user.getId()))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized user request failed");
        if(bill==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("bill doesn't exists in the data base");
        com.csye6225spring2020.entity.File file = fileRepositry.findById(fileId);
        if(file ==null)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file doesn't exists ");

        if(!file.getBillId().equals(billid)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("billid and file id doesn't match ");
        }
        String etxension = file.getFileName().substring(file.getFileName().lastIndexOf('.'));
        String fileOriginalName = file.getFileName().substring(0,file.getFileName().lastIndexOf('.'));
        Path path =Paths.get("tmp/"+fileOriginalName+"_"+billid+etxension);
        Files.delete(path);
        bill.setAttachment(null);
        billRepositry.save(bill);
        Long l = fileRepositry.deleteById(fileId);
        if(l==1)
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body("File is succsesfully deleted");
        else
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File is Not found");


    }

    private static String getFileChecksum(MessageDigest digest, File file) throws IOException
    {
        FileInputStream fis = new FileInputStream(file);
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        };
        fis.close();
        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i< bytes.length ;i++)
        {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }




}
