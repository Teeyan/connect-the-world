package com.example.joetian.connecttheword;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class Storage {
    private static FirebaseStorage storage = FirebaseStorage.getInstance();
    private static StorageReference rootRef = storage.getReference();

    public static StorageReference getRootRef() {
        return rootRef;
    }
}
