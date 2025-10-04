/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

/**
 *
 * @author Xthliene
 */
public class Tracker {
    Issue post;
    User manager;
    User handler;
    Status status;
}
enum Status {
    ASSIGNED,
    REPORTED,
    ONGOING,
    POSTPONED,
    FINISHED
}
