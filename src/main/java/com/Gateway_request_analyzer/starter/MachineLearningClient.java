package com.Gateway_request_analyzer.starter;


import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.redis.client.*;

import java.util.ArrayList;
import java.util.List;

/**
 * REQUIRED:
 * Server to send connections to ML-server (Websocket?)
 * Buffer to store up to 30 requests, then send continuously
 * Return value to allow blocking
 *
 * */
public class MachineLearningClient {

  private WebClient client;
  private RedisAPI redis;

  private static final int EXPIRY_3H = 10800;
  private static final long EXPIRY_3H_MILLIS = 10800000;

  public MachineLearningClient(RedisAPI redis, Vertx vertx){
    this.redis = redis;
    this.client = WebClient.create(vertx);
  }

  private JsonObject createJsonObject(String userID, String session, String URL){
    return new JsonObject()
      .put("timestamp", System.currentTimeMillis())
      .put("userID", userID)
      .put("sessionID", session)
      .put("expiring", System.currentTimeMillis() + EXPIRY_3H_MILLIS)
      .put("URL", URL);
  }

  public void insertRedisList(Event e){
    /**
     * Order to ML: timestamp, userID, sessionID, expiring, URL
     * */
    String ip = e.getIp();
    String userId = e.getUserId();
    String session = e.getSession();
    String URL = e.getURI();

    JsonObject jo = createJsonObject(userId, session, URL);
    String jsonString = jo.toString();

    List<String> insertion = new ArrayList<>();


    insertion.add(userId);
    insertion.add(jsonString);
    redis.llen(userId).onComplete(handler -> {
      int currentVal = handler.result().toInteger();
      System.out.println("Current length value for ML: " + currentVal);
      if(currentVal >= 49){

        redis.lpush(insertion).onFailure(err -> {
          System.out.println("Error adding element to Redis");
        });

        redis.ltrim(userId, "0", "49").onFailure(err -> {
          System.out.println("Error trimming list in MachineLearningClient");
        });

        sendPostRequest(userId);

      } else if(currentVal >= 20){
        redis.lpush(insertion, pushHandler -> {
          sendPostRequest(userId);
        });
      }
      else {

        redis.lpush(insertion, pushHandler -> {
          if(handler.result().toInteger() <= 1){
            System.out.println("Set expiry time");
            setRedisExpiry(userId);
          }
        });

      }
    }).onFailure(err -> {
      System.out.println("Error checking length of Redis List");
    });
  }

  private void setRedisExpiry(String userId){
    List<String> expTime = new ArrayList<>();
    expTime.add(userId);
    expTime.add(Integer.toString(EXPIRY_3H));
    redis.expire(expTime).onFailure(handler -> {
      System.out.println("Could not add Redis list expiry time");
    });
  }


  private void sendPostRequest(String userId){

    redis.lrange(userId, "0", "-1").onComplete(handler -> {
      JsonArray jo = new JsonArray(handler.result().toString());
      client
        .post(8090, "172.20.0.41", "/anomaly")
        .sendJson(jo)
        .onSuccess(res -> {
          System.out.println("This is response: " + res.bodyAsString());
          try{
            this.handleMlResponse(res.bodyAsJsonArray());
          } catch(Exception e){
            System.out.println("Error in sendPostRequest return val: " + e.getMessage());
          }
        });

    }).onFailure(err -> {
      System.out.println("Error fetching redis list");
    });
  }

  private void handleMlResponse(JsonArray jo){
    System.out.println("Result returned from ML: " + jo.toString());
  }

}
