package com.example.joetian.connecttheword;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.PriorityQueue;

public class ProfileFragment extends Fragment {

    static final int SELECT_PHOTO_REQUEST = 1;

    private int parentFrameHolder;
    private String uID;

    private ImageButton backBttn;
    private ImageView profilePic;
    private TextView usernameText;
    private TextView emailText;

    private DatabaseReference db;
    private PriorityQueue<MetaPage> pqueue;
    private MetaPage[] drawpList;
    private int numDisplay;

    private LayoutInflater inflater;

    /**
     * AGet an argumented instance of the fragment
     * @param parent integer denoting the id of the parent frmaelayout
     * @return new instance of DrawingFragment with relevant argument in its bundle
     */
    public static ProfileFragment newInstance(int parent) {
        ProfileFragment pfrag = new ProfileFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        pfrag.setArguments(args);
        return pfrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentFrameHolder = getArguments().getInt("parent");
        uID = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.profile_fragment, parent, false);
    }

    public void onViewCreated(View v, Bundle savedInstanceState) {
        //Back button
        backBttn = (ImageButton) v.findViewById(R.id.back_button);
        backBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                fm.popBackStackImmediate();
                ft.commit();
            }
        });

        //Text Information
        usernameText = (TextView) v.findViewById(R.id.username);
        usernameText.setText(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        emailText = (TextView) v.findViewById(R.id.email);
        emailText.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());

        //Profile Picture Image Button
        profilePic = (ImageView) v.findViewById(R.id.profile_picture);
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editProfilePicture();
            }
        });

        //Get DB instance and handle priority queue population
        db = FirebaseDatabase.getInstance().getReference();
        db.child("users").child(uID).child("profile_url").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    Picasso.with(getContext()).load(dataSnapshot
                            .getValue(String.class)).resize(300,300).into(profilePic);
                }
                else {
                    Picasso.with(getContext()).load(R.drawable.user_icon).resize(300,300).into(profilePic);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DB Request", databaseError.getMessage());
            }
        });

        pqueue = new PriorityQueue<MetaPage>(20, new MetaPage.MetaPageComparator());
        populateDrawps(v);
    }

    /**
     * Generate and handle dialog for editing user profile pictures. Upload to Firebase Storage / Database
     */
    private void editProfilePicture() {
        CharSequence[] options = {"Edit Profile Picture", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this.getContext());
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(which == 0) {
                    Intent photoSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    photoSelectionIntent.setType("image/*");
                    startActivityForResult(photoSelectionIntent, SELECT_PHOTO_REQUEST);
                }
                else {
                    ;
                }
            }
        });
        builder.show();
    }

    /**
     * Override activity result to get the photo selected by the user.
     * @param requestCode - request code from editProfilePicture
     * @param resultCode - result code for the request
     * @param data - data intent corresponding to selected image
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SELECT_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
            Uri selectedImg = data.getData();
            Bitmap imgBitmap;
            try {
                //Get Bitmap version of image and upload it
                imgBitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImg);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                imgBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                byte[] profileData = baos.toByteArray();

                StorageReference ref = Storage.getRootRef().child(uID).child(uID + ".jpg");
                UploadTask uploadTask = ref.putBytes(profileData);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.e("Upload Error", exception.getMessage());
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        db.child("users").child(uID).child("profile_url").setValue(downloadUrl.toString());
                    }
                });

            }catch(IOException e) {
                Toast.makeText(getActivity(), "File Not Found!", Toast.LENGTH_SHORT).show();
            }
        }
    }
    /**
     * Populate our priority queue with Drawps that a user has made in order of upvotes
     * @param v view instance that currently has focus for attribute access
     */
    private void populateDrawps(final View v) {
        db.child("users").child(uID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot loc : dataSnapshot.getChildren()) {
                    if(loc.getKey().equals("profile_url")) {continue;}
                    String location = loc.child("name").getValue(String.class);
                    String imgURL = loc.child("img_url").getValue(String.class);
                    Long numUpvotes = (Long)loc.child("num_upvotes").getValue();
                    Long numDownvotes = (Long)loc.child("num_downvotes").getValue();
                    MetaPage pInstance = new MetaPage(location, imgURL, numUpvotes.intValue(), numDownvotes.intValue());
                    pqueue.add(pInstance);
                    Log.d("RETREIVED DB", location + "," + imgURL + "," + numUpvotes.toString());
                }
                Log.d("NUM:","Num Entries Gotten: " + pqueue.size());
                drawpList = new MetaPage[pqueue.size()];
                numDisplay = pqueue.size();
                pqueue.toArray(drawpList);
                Log.d("sIE", String.valueOf(numDisplay));
                inflater = LayoutInflater.from(getActivity());
                ViewPager vp = (ViewPager)v.findViewById(R.id.pager);
                vp.setAdapter(new ProfileFragment.BrowsePageAdapter());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("DB Request", databaseError.getMessage());
            }
        });
    }

    class BrowsePageAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return numDisplay;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View page = inflater.inflate(R.layout.profile_browse_page, null);

            final MetaPage data = drawpList[position];
            ImageView drawing = (ImageView)page.findViewById(R.id.drawing);
            Picasso.with(getActivity()).load(data.getImgUrl()).resize(250,250).into(drawing);
            TextView imgInfo = (TextView)page.findViewById(R.id.img_info);
            TextView upvotes = (TextView)page.findViewById(R.id.upvote_num);
            TextView downvotes = (TextView)page.findViewById(R.id.downvote_num);

            imgInfo.setText(getString(R.string.general, data.getOwner()));
            upvotes.setText(getString(R.string.general, "Upvotes: "+String.valueOf(data.getNumUpvotes())));
            downvotes.setText(getString(R.string.general, "Downvotes: "+String.valueOf(data.getNumDownvotes())));

            ((ViewPager)container).addView(page, 0);
            return page;
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == (View)arg1;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            ((ViewPager)container).removeView((View)object);
            object = null;
        }
    }

}
