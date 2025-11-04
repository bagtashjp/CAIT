/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

import org.bson.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Xthliene
 */
public class Progress {
    User manager = null;
    User handler = null;
    String description = null;
    boolean isDone = false;
    public Progress(){}
    public Progress(Document doc, DragonBallZ db) {
        manager = db.getUser(doc.getObjectId("manager"));
        handler = db.getUser(doc.getObjectId("handler"));
        description = doc.getString("description");
        isDone = doc.getBoolean("is_done");
    }
    public Document toDoc() {
        return new Document()
            .append("manager", manager == null
                ? null
                : manager.id
            )
            .append("handler", handler == null
                ? null
                : handler.id
            )
            .append("description", description)
            .append("is_done", isDone);
    }
}

