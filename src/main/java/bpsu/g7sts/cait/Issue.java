/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

/**
 *
 * @author Xthliene
 */

public class Issue {
    int post_id;
    User user;
    String description;
    Severity severity;
    String[] tags;
    String[] imageURLs;
    boolean isTracked;
    Tracker tracker;
    boolean isClosed;
    CloseReason closeReason;
    User closer;
}

enum Severity {
    LOW,
    MEDIUM,
    HIGH
}

enum CloseReason {
    DENIED,
    INCOMPLETED,
    FINISHED
}