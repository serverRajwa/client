package com.example.addon;

public final class AuthManager {
   private AuthManager() {
   }

   public static boolean isAuthorized() {
      return true;
   }

   public static boolean isAuthFinished() {
      return true;
   }

   public static String getLastFailureReason() {
      return "";
   }
}
