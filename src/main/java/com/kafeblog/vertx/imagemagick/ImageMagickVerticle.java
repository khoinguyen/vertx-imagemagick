package com.kafeblog.vertx.imagemagick;

import java.io.*;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.platform.Verticle;

import org.vertx.java.core.buffer.Buffer;

import org.im4java.process.Pipe;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

public class ImageMagickVerticle extends Verticle {

  public void start() {


    vertx.eventBus().registerHandler("imagemagick.convert", new Handler<Message<String>>() {
      @Override
      public void handle(Message<String> message) {
        String actionStr = message.body();
        final String[] actions = actionStr.split(" ");
        message.reply("ok", new Handler<Message<Buffer>>() {
          @Override
          public void handle(Message<Buffer> message) {

            Buffer buffer = message.body();
            container.logger().info("Receive buffer for convert with size: " + buffer.length());
            if (buffer.length() <= 0) return;

            ByteArrayInputStream  is = new ByteArrayInputStream(buffer.getBytes());
            ByteArrayOutputStream os = new ByteArrayOutputStream();

            Pipe pipeIn  = new Pipe(is,null);
            Pipe pipeOut = new Pipe(null,os);

            IMOperation op = new IMOperation();
            op.addImage("-");    

            for (String action : actions) {
              String[] actionParams = action.split(":");
              if (actionParams.length == 0) continue;

              if (actionParams[0].equals("resample")) {
                if (actionParams.length != 2) continue;
                try {
                  op.resample(Integer.parseInt(actionParams[1]));  
                } catch(NumberFormatException nfe) {
                  String[] split = actionParams[1].toLowerCase().split("x");
                  try {
                    op.resample(Integer.parseInt(split[0]), Integer.parseInt(split[0]));
                  } catch( Exception ex) { /**/ }
                }
              }
            }

            op.addImage("-");

            
            try {
              ConvertCmd convert = new ConvertCmd();
              convert.setInputProvider(pipeIn);
              convert.setOutputConsumer(pipeOut);
              convert.run(op);  
  
              Buffer ob = new Buffer();
              ob.appendBytes(os.toByteArray());

              message.reply(ob);

            } catch (Exception ex) {
              container.logger().error(ex.getMessage(), ex);
            }
            

          }
        });
      }
    });
  }
}