package com.Gateway_request_analyzer.starter;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocket;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.client.*;
import io.vertx.redis.client.impl.RedisClient;

import java.util.HashMap;
import java.util.Objects;

public class GRAserver {

  Vertx vertx;
  RedisHandler redisHandler;
  Future<RedisConnection> sub;

  //Make this a HashMap
  private HashMap<String, ServerWebSocket> openConnections = new HashMap<>();

  public GRAserver(Vertx vertx, RedisHandler redisHandler, Future<RedisConnection> sub){
    this.vertx = vertx;
    this.redisHandler = redisHandler;
    this.sub = sub;
    this.createServer();

  }

  public void createServer(){

    subscriptionSetUp();

    vertx.createHttpServer().webSocketHandler(handler -> {
      System.out.println("Client connected: " + handler.binaryHandlerID());

      //socket = handler
      openConnections.put(handler.binaryHandlerID(), handler);

      handler.binaryMessageHandler(msg -> {
        JsonObject json = (JsonObject) Json.decodeValue(msg);
        Event event = new Event(json);
        redisHandler.eventRequest(event);
      });
        /*
        This is used when client disconnects,
        Remove connection from HashMap
        */
      handler.closeHandler(msg -> {
        openConnections.remove(handler.binaryHandlerID());
        System.out.println("Client disconnected" + handler.binaryHandlerID());
      });

    }).listen(3001).onSuccess(err -> {
      System.out.println("Connection to port succeeded");
    }).onFailure(err -> {
      System.out.println("Connection to port refused");
    });
  }

  private void subscriptionSetUp(){

    this.sub.onSuccess(conn ->{
      conn.send(Request.cmd(Command.SUBSCRIBE).arg("channel1"));
      conn.handler(message -> {

        Buffer buf;
        String str = message.toString();

        for(ServerWebSocket socket : openConnections.values()) {

          JsonObject json = new JsonObject();
          json.put("Action", str);
          buf = json.toBuffer();
          socket.writeBinaryMessage(buf);
        }
      });
    });
  }

}
