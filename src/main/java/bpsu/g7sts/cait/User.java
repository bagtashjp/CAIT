/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

/**
 *
 * @author Xthliene
 */


import org.bson.Document;
import org.bson.types.ObjectId;

public class User {
    ObjectId id;
    String username;
    String lastName;
    String firstName;
    ObjectId avatar;
    Role role = Role.USER;
    public User(){}
    public User(Document doc) {
        id = doc.getObjectId("_id");
        username = doc.getString("username");
        lastName = doc.getString("last_name");
        firstName = doc.getString("first_name");
        avatar = doc.getObjectId("avatar");
        role = Role.valueOf(doc.getString("role"));
    }
    public String getFullName() {
        return firstName + " " + lastName;
    }
}

enum Role {
    USER, 
    HANDLER,
    MANAGER,
    ADMIN
}