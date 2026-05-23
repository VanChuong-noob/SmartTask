package com.androidapp.SmartTask;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class FirebaseSyncManager {

    private static final String TAG = "FirebaseSync";
    private FirebaseFirestore db;
    private DatabaseHelper dbHelper;

    public FirebaseSyncManager() {
        db = FirebaseFirestore.getInstance();
    }

    public void setDbHelper(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public void uploadTask(Task task) {
        Map<String, Object> taskMap = new HashMap<>();
        taskMap.put("title", task.getTitle());
        taskMap.put("description", task.getDescription());
        taskMap.put("time", task.getTime());
        taskMap.put("date", task.getDate());
        taskMap.put("completed", task.isCompleted());
        taskMap.put("user_email", task.getUserEmail());
        taskMap.put("location_name", task.getLocationName());
        taskMap.put("location_lat", task.getLocationLat());
        taskMap.put("location_lng", task.getLocationLng());
        taskMap.put("location_reminder", task.isLocationReminder());

        db.collection("tasks")
                .document(String.valueOf(task.getId()))
                .set(taskMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Uploaded: " + task.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Upload failed", e));
    }

    public void deleteTask(int taskId) {
        db.collection("tasks")
                .document(String.valueOf(taskId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Deleted: " + taskId))
                .addOnFailureListener(e -> Log.e(TAG, "Delete failed", e));
    }

    public void syncTasks(String userEmail, SyncCallback callback) {
        db.collection("tasks")
                .whereEqualTo("user_email", userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Task task = new Task();
                        task.setId(Integer.parseInt(doc.getId()));
                        task.setTitle(doc.getString("title"));
                        task.setDescription(doc.getString("description"));
                        task.setTime(doc.getString("time"));
                        task.setDate(doc.getString("date"));
                        task.setCompleted(Boolean.TRUE.equals(doc.getBoolean("completed")));
                        task.setUserEmail(doc.getString("user_email"));
                        task.setLocationName(doc.getString("location_name"));
                        task.setLocationLat(doc.getDouble("location_lat") != null ? doc.getDouble("location_lat") : 0);
                        task.setLocationLng(doc.getDouble("location_lng") != null ? doc.getDouble("location_lng") : 0);
                        task.setLocationReminder(Boolean.TRUE.equals(doc.getBoolean("location_reminder")));

                        if (dbHelper != null) {
                            dbHelper.addTask(task);
                        }
                    }
                    callback.onSyncComplete();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Sync failed", e));
    }

    public interface SyncCallback {
        void onSyncComplete();
    }
}