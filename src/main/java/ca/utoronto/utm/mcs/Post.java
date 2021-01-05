package ca.utoronto.utm.mcs;

import static com.mongodb.client.model.Filters.eq;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class Post implements HttpHandler {

  MongoClient db;

  public Post(MongoClient db) {
    this.db = db;
  }

  @Override
  public void handle(HttpExchange r) {
    try {
      String reqMethod = r.getRequestMethod();
      if (reqMethod.equals("PUT")) {
        handlePut(r);
      } else if (reqMethod.equals("GET")) {
        handleGet(r);
      } else if (reqMethod.equals("DELETE")) {
        handleDelete(r);
      } else {
        r.sendResponseHeaders(405, -1);
      }
    } catch (JSONException l) {
      try {
        r.sendResponseHeaders(400, -1);
      } catch (IOException e) {
      }
    } catch (Exception e) {
      try {
        r.sendResponseHeaders(500, -1);
      } catch (IOException e1) {
      }
    }
  }

  // Method takes JSONArray and converts to array list to add to document
  private ArrayList<String> jsonArrayToList(JSONArray list) throws JSONException {
    ArrayList<String> regularArray = new ArrayList<>();
    if (list != null) {
      int len = list.length();
      for (int i = 0; i < len; i++) {
        regularArray.add(list.get(i).toString());
      }
    }
    return regularArray;
  }

  // Method gets the collection to use for posts
  private MongoCollection<Document> getCollection() {
    MongoDatabase database = db.getDatabase("csc301a2");
    try {
      database.createCollection("posts");
    } catch (Exception e) {
    }
    return database.getCollection("posts");
  }

  // Method creates the new document with headers and values given
  private Document createDocument(String[] headers, Object[] content) {
    ObjectId id = new ObjectId();
    Document post = new Document("_id", id);
    for (int i = 0; i < headers.length; i++) {
      post.append(headers[i], content[i]);
    }
    return post;
  }

  // Method writes response back to client with given hashmap of responses
  private void writeResponse(HttpExchange r, HashMap<String, Object> resultMap) throws IOException {
    JSONObject json = new JSONObject(resultMap);
    String response = json.toString();
    r.getResponseHeaders().set("Content-Type", "application/json");
    r.sendResponseHeaders(200, response.length());
    OutputStream os = r.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  // Handles PUT call from HTTP request
  public void handlePut(HttpExchange r) throws IOException, JSONException {
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    String title;
    String author;
    String content;
    JSONArray tags;
    
    // check if the query is formatted correctly with necessary parameters
    if (deserialized.has("title") && deserialized.has("author") && deserialized.has("content")
        && deserialized.has("tags")) {
      
      boolean error = false;
      
      // create a hashmap containing the response 
      HashMap<String, Object> resultMap;

      content = deserialized.getString("content");
      title = deserialized.getString("title");
      author = deserialized.getString("author");
      
      // if any of the title/content/author parameters are empty/null, send 400 error
      if (title.isEmpty() || content.isEmpty() || author.isEmpty()) {
        r.sendResponseHeaders(400, -1);
      } else {
        try {
          tags = new JSONArray(deserialized.getString("tags"));
          
          // get the existing collection of posts
          MongoCollection<Document> collection = getCollection();
          String[] headers = {"title", "author", "content", "tags"};
          Object[] values = {title, author, content, jsonArrayToList(tags)};
          
          // create a new post
          Document post = createDocument(headers, values);
          
          // insert the new post in the collection
          collection.insertOne(post);
          resultMap = new HashMap<>();
          
          // insert the post's id
          resultMap.put("_id", post.get("_id"));
        } catch (Exception e) {
          error = true;
          resultMap = null;
        }
        if (error) {
          
          // send 400 error if query is improperly formatted
          r.sendResponseHeaders(400, -1);
        } else {
          
          // send the response if query
          writeResponse(r, resultMap);
        }
      }
    }
    
    // send 400 error if query is improperly formatted or missing required information
    else {
      r.sendResponseHeaders(400, -1);
    }
  }
  
  // Handles DELETE call from HTTP request
  private void handleDelete(HttpExchange r) throws IOException, JSONException {
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    String criteria;
    Document removed;
    
    // check if the query is formatted correctly with necessary parameters
    if (deserialized.has("_id")) {
      
      // get the existing collection of posts
      MongoCollection<Document> collection = getCollection();
      criteria = deserialized.getString("_id");
      try {
        
        // try removing the post with the specified id in the collection
        removed = collection.findOneAndDelete(eq("_id", new ObjectId(criteria)));
      } catch (Exception e) {
        
        // if the query was incorrectly formatted, send 400 error
        r.sendResponseHeaders(400, -1);
        removed = null;
      }
      if (removed == null) {
        
        // if removed is null (aka the post with specified id does not exist), send 404 error
        r.sendResponseHeaders(404, -1);
      } else {
        
        // if post with specified id was successfully deleted, send 200 (no response body)
        r.sendResponseHeaders(200, -1);
        OutputStream os = r.getResponseBody();
        os.close();
      }
    } else {
      
      // send 400 error if query is improperly formatted or missing required information
      r.sendResponseHeaders(400, -1);
    }
  }

  // Handles GET call from HTTP request
  private void handleGet(HttpExchange r) throws IOException, JSONException {
    String body = Utils.convert(r.getRequestBody());
    JSONObject deserialized = new JSONObject(body);
    String criteria;
    boolean error = false;
    
    // check if the query is formatted correctly with necessary parameters
    if (deserialized.has("title") || deserialized.has("_id")) {
      
      // get the existing collection of posts
      MongoCollection<Document> collection = getCollection();
      FindIterable<Document> docIterable;
      Document toSort = new Document();
      toSort.append("title", 1);
      if (deserialized.has("_id")) {
        try {
          criteria = deserialized.getString("_id");
          docIterable = collection.find(eq("_id", new ObjectId(criteria))).sort(toSort);
        } catch (Exception e) {
          docIterable = null;
          error = true;
        }
      } else {
        criteria = deserialized.getString("title");
        Document regex = new Document();
        regex.append("$regex", ".*" + Pattern.quote(criteria) + ".*");
        Document toFind = new Document();
        toFind.append("title", regex);
        docIterable = collection.find(toFind).sort(toSort);

      }

      if (error) {
        r.sendResponseHeaders(400, -1);
      }

      else {
        MongoCursor<Document> iterator = docIterable.iterator();
        ArrayList<Object> retList = new ArrayList<>();
        HashMap<String, Object> id;
        HashMap<String, Object> document;


        while (iterator.hasNext()) {
          Document temp = iterator.next();
          id = new HashMap<>();
          document = new HashMap<>();
          id.put("$oid", temp.get("_id"));

          document.put("_id", new JSONObject(id));
          document.put("title", temp.get("title"));
          document.put("author", temp.get("author"));
          document.put("content", temp.get("content"));
          document.put("tags", temp.get("tags"));
          retList.add(new JSONObject(document));

        }

        if (retList.isEmpty()) {
          
          // if posts with specified criteria are not found in collection, send 404 error
          r.sendResponseHeaders(404, -1);
        } else {
          
          // send the sorted response body
          String response = retList.toString();
          r.getResponseHeaders().set("Content-Type", "application/json");
          r.sendResponseHeaders(200, response.length());
          OutputStream os = r.getResponseBody();
          os.write(response.getBytes());
          os.close();
        }
      }
    } else {
      
      // send 400 error if query is improperly formatted or missing required information
      r.sendResponseHeaders(400, -1);
    }
  }
}
