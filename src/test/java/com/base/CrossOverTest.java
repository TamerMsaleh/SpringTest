package com.base;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class CrossOverTest {
    @Test
    public void testNormalFlow() throws Exception{
        String inputChargeRequestRedis = "{\"serviceType\":\"voice\",\"unit\":2}";

        String resetRequestRedisResponse = sendResetRequest();
        Assertions.assertEquals(100, Integer.parseInt(resetRequestRedisResponse));

        String chargeRequestRedisResponse = sendChargeRequest( inputChargeRequestRedis);
        Assertions.assertEquals("{\"remainingBalance\":90,\"charges\":10,\"isAuthorized\":true}", chargeRequestRedisResponse);

        String chargeRequestRedisResponse1 = sendChargeRequest( inputChargeRequestRedis);
        Assertions.assertEquals("{\"remainingBalance\":80,\"charges\":10,\"isAuthorized\":true}", chargeRequestRedisResponse1);

        String chargeRequestRedisResponse2 = sendChargeRequest( inputChargeRequestRedis);
        Assertions.assertEquals("{\"remainingBalance\":70,\"charges\":10,\"isAuthorized\":true}", chargeRequestRedisResponse2);
    }

    @Test
    public void testConcurrentFlow() throws Exception{
        String inputChargeRequestVoice = "{\"serviceType\":\"voice\",\"unit\":2}";
        String inputChargeRequestData = "{\"serviceType\":\"data\",\"unit\":2}";

        // Test resetRequestRedis
        String resetRequestRedisResponse = sendResetRequest();
        Assertions.assertEquals(100, Integer.parseInt(resetRequestRedisResponse));


        sendChargeRequest( inputChargeRequestData); // remaining 80
        sendChargeRequest( inputChargeRequestData); // remaining 60
        sendChargeRequest( inputChargeRequestVoice); // remaining 50
        sendChargeRequest( inputChargeRequestVoice); // remaining 40
        sendChargeRequest( inputChargeRequestVoice); // remaining 30
        sendChargeRequest( inputChargeRequestData); // remaining 10
        String chargeRequestRedisResponse = sendChargeRequest( inputChargeRequestData);

        Assertions.assertEquals("{\"remainingBalance\":10,\"charges\":0,\"isAuthorized\":false}", chargeRequestRedisResponse);
        sendResetRequest();

    }

    static final String API_GATEWAY_URL = "https://d4z3p89mjk.execute-api.us-east-1.amazonaws.com/prod";

    private String sendChargeRequest(String body) throws Exception{
        try {
            URL url = new URL(API_GATEWAY_URL + "/charge-request-redis");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            connection.getOutputStream().write(body.getBytes());

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                System.out.println("Request failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String sendResetRequest() {
        try {
            URL url = new URL(API_GATEWAY_URL + "/reset-redis");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                return response.toString();
            } else {
                System.out.println("Request failed with response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

}
