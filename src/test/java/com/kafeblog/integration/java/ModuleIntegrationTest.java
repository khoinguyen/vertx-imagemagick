package com.kafeblog.integration.java;
/*
 * Copyright 2013 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpClientResponse;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;
import org.vertx.java.core.buffer.Buffer;
import java.io.*;
import static org.vertx.testtools.VertxAssert.*;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.file.*;
/**
 * Example Java integration test that deploys the module that this project builds.
 *
 * Quite often in integration tests you want to deploy the same module for all tests and you don't want tests
 * to start before the module has been deployed.
 *
 * This test demonstrates how to do that.
 */
public class ModuleIntegrationTest extends TestVerticle {
  @Test
  public void testResample() {
    container.logger().info("Resampling");
    vertx.eventBus().send("imagemagick.convert", "resample:300", new Handler<Message<String>>(){
      @Override
      public void handle(Message<String> reply) {
        assertEquals("ok", reply.body());
        final Class thisClass = getClass();
        container.logger().info(thisClass.getResource("/input.jpg"));
        InputStream in = thisClass.getResourceAsStream("/input.jpg");

        byte[] bytes = new byte[8000];
        int bytesRead = 0;
        Buffer buffer = new Buffer();
        try {
          while((bytesRead = in.read(bytes)) != -1) {
            buffer.appendBytes(bytes);
          }  
        } catch (IOException ioe) {
          container.logger().error(ioe);
        }
        

        reply.reply(buffer, new Handler<Message<Buffer>>() {
          @Override
          public void handle(Message<Buffer> converted) {
            final Buffer result = converted.body();

            InputStream in2 = thisClass.getResourceAsStream("/output.jpg");
            byte[] bytes = new byte[8000];
            int bytesRead = 0;
            int totalBytesRead = 0;
            // Buffer buffer = new Buffer();
            try {

              while((bytesRead = in2.read(bytes)) != -1) {
                totalBytesRead += bytesRead;
              }

            } catch (IOException ioe) {
              container.logger().error(ioe);
            }

            assertEquals(result.length(), totalBytesRead);

            final Logger log = container.logger();
            log.info("Sample output size: " + totalBytesRead + ". Result size: " + result.length());
            testComplete();
          }
        });
      }
    } );
  }

  @Override
  public void start() {
    // Make sure we call initialize() - this sets up the assert stuff so assert functionality works correctly
    initialize();
    // Deploy the module - the System property `vertx.modulename` will contain the name of the module so you
    // don't have to hardecode it in your tests
    container.deployModule(System.getProperty("vertx.modulename"), new AsyncResultHandler<String>() {
      @Override
      public void handle(AsyncResult<String> asyncResult) {
      // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
      assertTrue(asyncResult.succeeded());
      assertNotNull("deploymentID should not be null", asyncResult.result());
      // If deployed correctly then start the tests!
      startTests();
      }
    });
  }

}
