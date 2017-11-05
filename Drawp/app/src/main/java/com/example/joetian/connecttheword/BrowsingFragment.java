package com.example.joetian.connecttheword;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.support.v4.view.PagerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.*;
import java.util.PriorityQueue;
import android.net.Uri;
import com.squareup.picasso.Picasso;

public class BrowsingFragment extends Fragment {

    private int numDisplay = 10; //number of images to view at a time

    private PriorityQueue<MetaPage> pqueue; //priority queue to contain meta pages

    private DatabaseReference db; //database instance for retrieving information

    private int parentFrameHolder;
    private String locationId;

    private LayoutInflater inflater;

    private ImageButton backBttn;

    /**
     * Set up new instance of browsing fragment with parent frame holder
     * @param parent int representing id of the frame layout this fragment is being put into
     * @return new instance of mapfragment with arguments available
     */
    public static BrowsingFragment newInstance(int parent, String locId) {
        BrowsingFragment bfrag = new BrowsingFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        args.putString("lID", locId);
        bfrag.setArguments(args);
        return bfrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Browsing:", "STARTING BROWSING LIST");
        super.onCreate(savedInstanceState);
        parentFrameHolder = getArguments().getInt("parent");
        locationId = getArguments().getString("lID");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.browsing_fragment, parent, false);
        return v;
    }

    @Override
    public void onViewCreated(View v, Bundle savedInstanceState) {
        backBttn = (ImageButton) v.findViewById(R.id.back_button);
        backBttn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                MapFragment mfrag = MapFragment.newInstance(parentFrameHolder);
                ft.hide(BrowsingFragment.this);
                ft.replace(parentFrameHolder, mfrag);
                ft.commit();
            }
        });
        db = FirebaseDatabase.getInstance().getReference();
        pqueue = new PriorityQueue<MetaPage>(20, new MetaPage.MetaPageComparator());
        getLocationInformation(v);
    }

    private void getLocationInformation(final View v) {
        db.child("locations").child(locationId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot user : dataSnapshot.getChildren()) {
                    String owner = user.getKey();
                    String imgURL = user.child("img_url").getValue(String.class);
                    Long numUpvotes = (Long)user.child("num_upvotes").getValue();
                    Long numDownvotes = (Long)user.child("num_downvotes").getValue();
                    MetaPage pInstance = new MetaPage(owner, imgURL, numUpvotes.intValue(), numDownvotes.intValue());
                    pqueue.add(pInstance);
                    Log.d("RETREIVED DB", owner + "," + imgURL + "," + numUpvotes.toString());
                }
                inflater = LayoutInflater.from(getActivity());
                ViewPager vp = (ViewPager)v.findViewById(R.id.pager);
                vp.setAdapter(new BrowsePageAdapter());
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
            return pqueue.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View page = inflater.inflate(R.layout.browse_page, null);

            MetaPage data = pqueue.poll();
            final ImageView drawing = (ImageView)page.findViewById(R.id.drawing);
            Storage.getRootRef().child(data.getImgUrl()).getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    Picasso.with(getActivity()).load(uri).resize(500,500).into(drawing);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    Log.e("Getting JPG ERROR", exception.getMessage());
                }
            });
            TextView imgInfo = (TextView)page.findViewById(R.id.img_info);
            ImageButton upBttn = (ImageButton)page.findViewById(R.id.upvote);
            TextView upNum = (TextView)page.findViewById(R.id.upvote_num);
            ImageButton downBttn = (ImageButton)page.findViewById(R.id.downvote);
            TextView downNum = (TextView)page.findViewById(R.id.downvote_num);

            imgInfo.setText(getString(R.string.author_browse, data.getOwner()));
            upNum.setText(String.valueOf(data.getNumUpvotes()));
            downNum.setText(String.valueOf(data.getNumDownvotes()));

            ((ViewPager)container).addView(page, 0);
            notifyDataSetChanged();
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
