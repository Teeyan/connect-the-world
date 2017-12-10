package com.example.joetian.connecttheword;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.os.Bundle;
import com.google.firebase.auth.UserProfileChangeRequest;

public class AuthFragment extends Fragment{

    private FirebaseAuth firebaseAuth;

    private int parentFrameHolder;

    private EditText emailEditText;
    private EditText passEditText;

    private Button loginBttn;
    private Button createBttn;

    /**
     * Set up new instance of map fragment with parent frame holder
     * @param parent int representing id of the frame layout this fragment is being put into
     * @return new instance of mapfragment with arguments available
     */
    public static AuthFragment newInstance(int parent) {
        AuthFragment afrag = new AuthFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        afrag.setArguments(args);
        return afrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        parentFrameHolder = getArguments().getInt("parent");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.auth_fragment, parent, false);
        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        emailEditText = (EditText) v.findViewById(R.id.login_email);
        passEditText = (EditText) v.findViewById(R.id.login_password);

        //On Click Handling for Login Button
        loginBttn = (Button) v.findViewById(R.id.login_button);
        loginBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               loginUser();
            }
        });

        //On Click Handling for Create Button
        createBttn = (Button) v.findViewById(R.id.create_button);
        createBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAccount();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        //Check if the user is currently signed in and update accordingly
        FirebaseUser currUser = firebaseAuth.getCurrentUser();
        if(currUser != null) {
            FragmentManager fm = getFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
            ft.hide(AuthFragment.this);
            ft.replace(parentFrameHolder, mfrag);
            ft.commit();
        }
    }

    /**
     * Login the User using Firebase Auth Credentials from given information.
     */
    private void loginUser() {
        //Get user inputted email and password
        String email = emailEditText.getText().toString();
        String password = passEditText.getText().toString();
        //Pass to FirebaseAuth for Authentication
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //Success! Move to the new UI view
                        if (task.isSuccessful()) {
                            FragmentManager fm = getFragmentManager();
                            FragmentTransaction ft = fm.beginTransaction();
                            MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
                            ft.hide(AuthFragment.this);
                            //ft.replace(parentFrameHolder, mfrag, "Map");
                            ft.add(parentFrameHolder, mfrag, "Map");
                            ft.addToBackStack(null);
                            ft.commit();
                        }
                        //Error Logging In, display message
                        else {
                            Toast.makeText(getActivity(), "Invalid Email or Password",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Create an Account with the given information. Query for User display name and pass to Firebase Auth
     */
    private void createAccount() {
        final String email = emailEditText.getText().toString();
        final String password = passEditText.getText().toString();

        //Get Username Information Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Enter User Display Name");
        final EditText input = new EditText(getActivity());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        builder.setPositiveButton("Create!", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String displayName = input.getText().toString();

                //Pass to FirebaseAuth for account creation
                firebaseAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                //Success move to the new ui view
                                if(task.isSuccessful()) {
                                    setDisplayName(displayName);
                                }
                                //Error creating account, display message
                                else {
                                    Toast.makeText(getActivity(), "Account Creation Failure",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    private void setDisplayName(String displayName) {
        UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder();
        builder.setDisplayName(displayName);
        firebaseAuth.getCurrentUser().updateProfile(builder.build()).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(getActivity(), "Account Created!", Toast.LENGTH_SHORT).show();
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
                ft.hide(AuthFragment.this);
                ft.replace(parentFrameHolder, mfrag);
                ft.commit();
            }
        });
    }

}
