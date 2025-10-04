/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;

/**
 *
 * @author Xthliene
 */
public class User {
    int id;
    int lastName;
    int firstName;
    int middleName;
    int role;
}

enum Role {
    USER, 
    HANDLER,
    MANAGER,
    ADMIN
}