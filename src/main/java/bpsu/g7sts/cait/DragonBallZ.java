/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.types.ObjectId;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSUploadOptions;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import java.awt.Image;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.ImageIcon;
import org.bson.BsonValue;
import org.bson.conversions.Bson;

/**
 *
 * @author Xthliene
 */
public class DragonBallZ {

    private final MongoDatabase db;
    private final MongoClient client;
    private final MongoCollection<Document> users;
    private final MongoCollection<Document> posts;
    private final MongoCollection<Document> tracked_posts;
    private final GridFSBucket imageBucket;

    /**
     * Constructor: Initializes the database helper.
     *
     * @param databaseName The name of the Database
     */
    public DragonBallZ(String databaseName) {
        client = MongoClients.create("mongodb://localhost:27017");
        db = client.getDatabase(databaseName);
        users = db.getCollection("users");
        posts = db.getCollection("posts");
        tracked_posts = db.getCollection("tracked_posts");
        imageBucket = GridFSBuckets.create(db, "post_images");
    }

    /**
     * Returns the very database
     *
     * @return The database
     */
    public MongoDatabase raw() {
        return db;
    }

    public void insertDoc(MongoCollection<Document> collection, Document doc) {
        collection.replaceOne(Filters.eq("_id", doc.get("_id")), doc, new ReplaceOptions().upsert(true));
    }

    public void deleteDoc(MongoCollection<Document> collection, Document doc) {
        collection.deleteOne(Filters.eq("_id", doc.get("_id")));
    }

    public ObjectId addUser(Document doc) {
        BsonValue id = users.insertOne(doc).getInsertedId();
        if (id == null) {
            return null;
        }
        return id.asObjectId().getValue();

    }

    public Document getUserDoc(String username) {
        Document doc = users.find(eq("username", username)).first();
        return doc;
    }

    public User getUser(ObjectId id) {
        Document doc = users.find(eq("_id", id)).first();
        if (doc == null) {
            return null;
        }
        return getUser(doc);
    }

    public User getUser(String username) {
        Document doc = getUserDoc(username);
        if (doc == null) {
            return null;
        }
        return getUser(doc);
    }

    public User getUser(Document doc) {
        User user = new User(doc);
        return user;
    }

    public Document getPostDoc(ObjectId id) {
        return posts.find(eq("_id", id)).first();
    }

    public ObjectId addPost(Document doc) {
        BsonValue id = posts.insertOne(doc).getInsertedId();
        if (id == null) {
            return null;
        }
        return id.asObjectId().getValue();
    }

    public void updatePost(ObjectId id, Document doc) {
        try {
            posts.updateOne(eq("_id", id), doc);
            System.out.println("Successfully updated Issue: " + id.toHexString());
        } catch (Exception e) {
            System.err.println("Failed to update Issue " + id.toHexString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updatePost(ObjectId id, Bson bson) {
        try {
            posts.updateOne(eq("_id", id), bson);
            System.out.println("Successfully updated Issue: " + id.toHexString());
        } catch (Exception e) {
            System.err.println("Failed to update Issue " + id.toHexString() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updateProgressDone(ObjectId postId, int index) {
        try {
            // Perform the update
            UpdateResult result = posts.updateOne(
                    Filters.eq("_id", postId),
                    Updates.set("progress." + index + ".is_done", true)
            );

            // Check if any document was actually modified
            if (result.getModifiedCount() > 0) {
                System.out.println("✅ Successfully updated progress[" + index + "] to done for post " + postId);
            } else if (result.getMatchedCount() > 0) {
                System.out.println("⚠️ Document found, but progress index might be out of range.");
            } else {
                System.out.println("❌ No document found with _id " + postId);
            }

        } catch (Exception err) {
            System.err.println("🔥 Error updating progress[" + index + "] for post " + postId + ": " + err.getMessage());
            err.printStackTrace();
        }
    }

    public boolean isUserExist(String username) {
        return users.find(eq("username", username)).first() != null;
    }

    public ObjectId uploadImage(File file) {
        try (FileInputStream stream = new FileInputStream(file)) {
            ObjectId fileId = imageBucket.uploadFromStream(file.getName(), stream);
            System.out.println("Stored file in GridFS with id: " + fileId);
            return fileId;
        } catch (IOException e) {
            System.err.println("IO error while uploading image: " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    public ImageIcon getImage(ObjectId id) {
        return getImage(id, 100);
    }

    public ImageIcon getImage(ObjectId id, int targetHeight) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBucket.downloadToStream(id, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            ImageIcon icon = new ImageIcon(imageBytes);
            Image image = icon.getImage();

            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);

            if (originalHeight == 0) {
                // Fallback to avoid division by zero
                return icon;
            }

            // Scale width to maintain aspect ratio
            double scale = (double) targetHeight / originalHeight;
            int newWidth = (int) (originalWidth * scale);

            Image scaledImage = image.getScaledInstance(newWidth, targetHeight, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ImageIcon getOriginalImage(ObjectId id) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            imageBucket.downloadToStream(id, outputStream);
            byte[] imageBytes = outputStream.toByteArray();

            ImageIcon icon = new ImageIcon(imageBytes);
            Image image = icon.getImage();

            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);

            // ✅ If image height is larger than 700, scale it down while keeping aspect ratio
            if (originalHeight > 900) {
                double scale = 900.0 / originalHeight;
                int newWidth = (int) (originalWidth * scale);
                Image scaledImage = image.getScaledInstance(newWidth, 900, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            } else {
                // ✅ Otherwise, just return the original image
                return icon;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public ImageIcon[] getImages(Document doc) {
        return new ImageIcon[]{};
    }

    public ArrayList<Document> getPostDocs(int page) {
        return posts.find().sort(Sorts.descending("created_at")).skip((page - 1) * 20).limit(20).into(new ArrayList<>());
    }

    public ArrayList<Issue> getPosts(int page) {
        ArrayList<Issue> postIssues = new ArrayList<>();
        for (Document post : getPostDocs(page)) {
            Issue a = new Issue(post, this);
            postIssues.add(a);
        }
        return postIssues;
    }

    /**
     * Use to move doc from one collection to another.
     *
     * @param source The source collection.
     * @param target The target collection.
     * @param filter Filters to point to the document to move.
     * @return <code>false</code> if the document is not found based on the
     * filter and <code>true</code> if document is successfully moved.
     */
    public boolean moveDoc(
            MongoCollection<Document> source,
            MongoCollection<Document> target,
            Document filter
    ) {
        Document docToMove = source.find(filter).first();
        if (docToMove == null) {
            System.out.println("No document found matching the filter.");
            return false;
        }
        insertDoc(target, docToMove);
        deleteDoc(source, docToMove);

        System.out.println("Moved document with _id: " + docToMove.get("_id"));
        return true;
    }

}
