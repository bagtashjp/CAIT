/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 *
 * @author Xthliene
 */
public class Issue {
    DragonBallZ db;
    ObjectId id;
    Date created_at = new Date();
    User user;
    String title;
    String description;
    Severity severity;
    String department;
    List<String> tags;
    List<ObjectId> images = new ArrayList<>();
    Status status = Status.OPEN;
    List<Progress> progress = new ArrayList<>();
    String closeReason = null;
    User closer = null;
    int review_score = 0;
    String review_description = null;

    public Issue(DragonBallZ database) {
        db = database;
    }

    public Issue(Document doc, DragonBallZ database) {
        db = database;
        selfDoc(doc);
    }

    private void selfDoc(Document doc) {
        id = doc.getObjectId("_id");
        created_at = doc.getDate("created_at");
        user = db.getUser(doc.getObjectId("user"));
        title = doc.getString("title");
        description = doc.getString("description");
        tags = doc.getList("tags", String.class);
        severity = Severity.valueOf(doc.getString("severity"));
        department = doc.getString("department");
        images = doc.getList("images", ObjectId.class);
        status = Status.valueOf(doc.getString("status"));
        for (Document progressDoc : doc.getList("progress", Document.class)) {
            progress.add(new Progress(progressDoc, db));
        }
        closeReason = doc.getString("close_reason");
        closer = (doc.getObjectId("closer") == null)
                ? null
                : db.getUser(doc.getObjectId("closer")
                );
        review_score = doc.getInteger("review_score");
        review_description = doc.getString("review_description");
    }

    public Document toDoc() {
        List<Document> progressDoc = new ArrayList<>();
        for (Progress prog : progress) {
            progressDoc.add(prog.toDoc());
        }
        return new Document()
                .append("created_at", created_at)
                .append("user", user.id)
                .append("title", title)
                .append("description", description)
                .append("severity", severity.toString())
                .append("department", department)
                .append("tags", tags)
                .append("images", images)
                .append("status", status.toString())
                .append("progress", progressDoc)
                .append("close_reason", closeReason)
                .append("closer", (closer == null)
                        ? null
                        : closer.id
                )
                .append("review_score", review_score)
                .append("review_description", review_description);
    }

    public Status getStatus() {
        if (status == Status.OPEN && !progress.isEmpty()) {
            return Status.ONGOING;
        } else {
            return status;
        }
    }

    public boolean isAllDone() {
        for (Progress prog : progress) {
            if (!prog.isDone) {
                return false;
            }
        }
        return true;
    }

    public void selfUpdate() {
        selfDoc(db.getPostDoc(id));
    }
}

enum Severity {
    LOW,
    MEDIUM,
    HIGH,
    SEVERE
}

enum Status {
    OPEN,
    ONGOING,
    // Closure statuses
    RESOLVED,
    REJECTED,
    WITHDRAWN,
    CANCELED
}
