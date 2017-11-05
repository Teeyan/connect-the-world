package com.example.joetian.connecttheword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.ViewGroup;
import android.view.View;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.View.OnClickListener;
import java.io.ByteArrayOutputStream;
import android.graphics.Bitmap;
import android.app.Dialog;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.TextView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;
import android.support.annotation.NonNull;
import android.net.Uri;
import android.util.Log;

public class DrawingFragment extends Fragment implements OnClickListener {

    //metadata content
    private int parentFrameHolder;
    private String locID;
    private String uID;
    private String locName;

    private TextView locTitle;

    //canvas
    private LinearLayout paintLayout; //upper row of paint colors
    private LinearLayout paintLayoutBttm; //bottom row of paint colors
    private CanvasView canvasView; // custom canvas view that user will interact with
    //different brush sizes
    private float smallBrush, mediumBrush, largeBrush; //floats denoting dp brush sizes

    private ImageButton eraseBttn;
    private ImageButton drawBttn;
    private ImageButton currPaint;
    private ImageButton backBttn;
    private ImageButton uploadBttn;

    /**
     * AGet an argumented instance of the fragment
     * @param parent integer denoting the id of the parent frmaelayout
     * @param locId String denoting the point of interest we are drawping for
     * @param userId String denoting the userId of the user making the drawp
     * @return new instance of DrawingFragment with relevant argument in its bundle
     */
    public static DrawingFragment newInstance(int parent, String locId, String userId, String locName) {
        DrawingFragment dfrag = new DrawingFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        args.putString("lID", locId);
        args.putString("uID", userId);
        args.putString("locName", locName);
        dfrag.setArguments(args);
        return dfrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentFrameHolder = getArguments().getInt("parent");
        locID = getArguments().getString("lID");
        uID = getArguments().getString("uID");
        locName = getArguments().getString("locName");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.drawing_fragment, parent, false);
    }

    public void onViewCreated(View v, Bundle savedInstanceState) {
        canvasView = (CanvasView)v.findViewById(R.id.canvas);
        canvasView.setBrushSize(mediumBrush);

        //Initialize paint color buttons
        paintLayout = (LinearLayout)v.findViewById(R.id.paint_colors);
        initializeChildPaintButtons(paintLayout);
        paintLayoutBttm = (LinearLayout)v.findViewById(R.id.paint_colors_bottom);
        initializeChildPaintButtons(paintLayoutBttm);

        //Set current paint to default button
        currPaint = (ImageButton)paintLayout.getChildAt(0);
        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));

        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        drawBttn = (ImageButton)v.findViewById(R.id.draw_btn);
        drawBttn.setOnClickListener(this);

        eraseBttn = (ImageButton)v.findViewById(R.id.erase_btn);
        eraseBttn.setOnClickListener(this);

        locTitle = (TextView)v.findViewById(R.id.location_title);
        locTitle.setText(locName);

        backBttn = (ImageButton)v.findViewById(R.id.back_button);
        backBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
                ft.hide(DrawingFragment.this);
                ft.replace(parentFrameHolder, mfrag);
                ft.commit();
            }
        });

        uploadBttn = (ImageButton)v.findViewById(R.id.upload_btn);
        uploadBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Dialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle("Confirm :").setMessage("Are you ready to make a Drawp?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    canvasView.setDrawingCacheEnabled(true);
                    canvasView.buildDrawingCache();
                    Bitmap bitmap = canvasView.getDrawingCache();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] data = baos.toByteArray();

                    StorageReference ref = Storage.getRootRef().child(locID).child(uID).child(locID + "_" + uID + ".jpg");
                    UploadTask uploadTask = ref.putBytes(data);
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
                            DatabaseReference db = FirebaseDatabase.getInstance().getReference();
                            db.child("locations").child(locID).child(uID).child("img_url").setValue(downloadUrl.toString());
                            db.child("locations").child(locID).child(uID).child("num_downvotes").setValue(0);
                            db.child("locations").child(locID).child(uID).child("num_upvotes").setValue(0);

                            FragmentManager fm = getFragmentManager();
                            FragmentTransaction ft = fm.beginTransaction();
                            MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
                            ft.hide(DrawingFragment.this);
                            ft.replace(parentFrameHolder, mfrag);
                            ft.commit();
                        }
                    });
                }}).setNegativeButton(android.R.string.no, null).show();
            }
        });
    }

    /**
     * Set onclick listener to this instance fragment for all paint color image buttons
     * @param parentLayout - layout instance that containts paint color image buttons
     */
    private void initializeChildPaintButtons(LinearLayout parentLayout) {
        for(int i = 0; i < parentLayout.getChildCount(); i++) {
            ImageButton b = (ImageButton)parentLayout.getChildAt(i);
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view != currPaint) {
                        ImageButton imgView = (ImageButton) view;
                        String color = view.getTag().toString();
                        canvasView.setColor(color);
                        imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
                        currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
                        currPaint=(ImageButton)view;
                        canvasView.setBrushSize(canvasView.getLastBrushSize());
                        canvasView.setErase(false);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View view) {
        //Handle draw button on click
        if(view.getId() == R.id.draw_btn) {
            final Dialog brushDialog = new Dialog(getActivity());
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBttn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setBrushSize(smallBrush);
                    canvasView.setLastBrushSize(smallBrush);
                    canvasView.setErase(false);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBttn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setBrushSize(mediumBrush);
                    canvasView.setLastBrushSize(mediumBrush);
                    canvasView.setErase(false);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBttn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setBrushSize(largeBrush);
                    canvasView.setLastBrushSize(largeBrush);
                    canvasView.setErase(false);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        }
        //handle erase button click
        else if(view.getId()==R.id.erase_btn){
            final Dialog brushDialog = new Dialog(getActivity());
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBttn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setErase(true);
                    canvasView.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBttn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setErase(true);
                    canvasView.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBttn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBttn.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    canvasView.setErase(true);
                    canvasView.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        }
    }
/*
    public void paintClicked(View view) {
        if (view != currPaint) {
            ImageButton imgView = (ImageButton) view;
            String color = view.getTag().toString();
            canvasView.setColor(color);
            imgView.setImageDrawable(getResources().getDrawable(R.drawable.paint_pressed));
            currPaint.setImageDrawable(getResources().getDrawable(R.drawable.paint));
            currPaint=(ImageButton)view;
            canvasView.setBrushSize(canvasView.getLastBrushSize());
            canvasView.setErase(false);
        }
    }*/

}
