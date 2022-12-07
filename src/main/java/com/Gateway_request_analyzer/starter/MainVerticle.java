package com.Gateway_request_analyzer.starter;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {
  @Override
  public void start(){

    JsonObject jo = new JsonObject();
    jo.put("port", 3000);
    jo.put("host", "localhost");

    vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(jo));

    JsonObject jo2 = new JsonObject();
    jo2.put("port", 3001);
    jo2.put("host", "localhost");

    vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(jo2));

    JsonObject jo3 = new JsonObject();
    jo3.put("port", 3002);
    jo3.put("host", "localhost");

    vertx.deployVerticle(new ServerVerticle(), new DeploymentOptions().setConfig(jo3));

  }


}