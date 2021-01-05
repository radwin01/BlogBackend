package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.inject.Singleton;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;
import dagger.Module;
import dagger.Provides;

@Module
public class DaggerModule {

  private static HttpServer server;
  private static MongoClient db;

  @Provides
  @Singleton
  public MongoClient provideMongoClient() {
    /* TODO: Fill in this function */
    db = MongoClients.create("mongodb://localhost");
    MongoDatabase database = db.getDatabase("csc301a2");
    try {
      database.createCollection("posts");
    } catch (Exception e) {
      database.getCollection("posts");
    }
    return db;


  }

  @Provides
  public Post buildPost() {
    return new Post(db);
  }


  @Provides
  @Singleton
  public HttpServer provideHttpServer() {
    /* TODO: Fill in this function */
    try {
      server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8080), 0);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return server;
  }
}
