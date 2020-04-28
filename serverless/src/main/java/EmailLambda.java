import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.UUID;

public class EmailLambda  implements RequestHandler<SNSEvent, Object> {
    static DynamoDB dynamoDB;
    public Object handleRequest(SNSEvent request, Context context) {

        context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
        String domain = System.getenv("domain");
        context.getLogger().log("Domain : " + domain);
        final String FROM = "no-reply@" + domain;
        String billPayload = request.getRecords().get(0).getSNS().getMessage();
        JsonObject jsonObject = new JsonParser().parse(billPayload).getAsJsonObject();
        context.getLogger().log("Constructing my string for you to see");
        String TO = jsonObject.get("email").getAsString();
        JsonArray listBillId =jsonObject.getAsJsonArray("dueBillIds");
        String emailVal = appendMessage(listBillId,domain);
        context.getLogger().log(emailVal);
        try {
            init();
            Table table = dynamoDB.getTable("csye6225");
            long unixTime = Instant.now().getEpochSecond()+60*60;
            long now = Instant.now().getEpochSecond();
            context.getLogger().log(unixTime + " calculated time");
            context.getLogger().log(Instant.now().getEpochSecond() + " current time ");
            if (table == null) {
                context.getLogger().log("table not found");
            } else {
                Item item = table.getItem("email", TO);
                if (item == null || (item!=null && Long.parseLong(item.get("ttlInMin").toString())< now)) {
                    String token = UUID.randomUUID().toString();
                    Item itemPut = new Item()
                            .withPrimaryKey("email", TO)
                            .withNumber("ttlInMin", unixTime);

                    table.putItem(itemPut);

                    AmazonSimpleEmailService client =
                            AmazonSimpleEmailServiceClientBuilder.standard()
                                    .withRegion(Regions.US_EAST_1).build();
                    SendEmailRequest req = new SendEmailRequest()
                            .withDestination(
                                    new Destination()
                                            .withToAddresses(TO))
                            .withMessage(
                                    new Message()
                                            .withBody(
                                                    new Body()
                                                            .withHtml(
                                                                    new Content()
                                                                            .withCharset(
                                                                                    "UTF-8")
                                                                            .withData(
                                                                                    "Please find the BIll items which are due <br>" +
                                                                                            emailVal))
                                            )
                                            .withSubject(
                                                    new Content().withCharset("UTF-8")
                                                            .withData("List Of Due Bills")))
                            .withSource(FROM);
                    SendEmailResult response = client.sendEmail(req);
                    System.out.println("Email sent!");
                } else {
                    context.getLogger().log(item.toJSON() + "Email Already triggered!");
                }
            }
        } catch (Exception ex) {
            context.getLogger().log("The email was not sent. Error message: "
                    + ex.getMessage());
        }

        return null;
    }

    private static void init() throws Exception {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withRegion(Regions.US_EAST_1)
                .build();
        dynamoDB = new DynamoDB(client);
    }

    public String appendMessage(JsonArray listBillId,String domain){
        String emailTxt = "";
        for(int i = 0; i<listBillId.size();i++){
            String temp = listBillId.get(i).toString();
            emailTxt += "<p><a href='#'>https://" + domain + "/v1/bill/"+ temp +"</a></p><br>";
            emailTxt =  emailTxt.replaceAll("\"","");
        }
        return emailTxt;
    }
}