/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package bpsu.g7sts.cait;


import at.favre.lib.crypto.bcrypt.BCrypt;

public class Passworder {

    public static String hashPassword(String password) {
        return hashPassword(password.toCharArray());
    }

    public static String hashPassword(char[] password) {
        return BCrypt.withDefaults().hashToString(12, password);
    }

    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        return verifyPassword(plainPassword.toCharArray(), hashedPassword);
    }

    public static boolean verifyPassword(char[] plainPassword, String hashedPassword) {
        if (hashedPassword == null || hashedPassword.isEmpty()) {
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword, hashedPassword);
        return result.verified;
    }
}

