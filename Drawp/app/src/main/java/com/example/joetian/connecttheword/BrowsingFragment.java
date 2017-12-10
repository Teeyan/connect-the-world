package com.example.joetian.connecttheword;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.*;
import android.support.v4.view.PagerAdapter;
import com.google.firebase.database.*;
import java.util.PriorityQueue;
import com.squareup.picasso.Picasso;

public class BrowsingFragment extends Fragment {

    private int numDisplay; //number of images to view at a time

    private PriorityQueue<MetaPage> pqueue; //priority queue to contain meta pages
    private MetaPage[] drawpList; //MetaPage array for usage with the priority queue

    private DatabaseReference db; //database instance for retrieving information

    private int parentFrameHolder;
    private String locationId;
    private String locationName;

    private LayoutInflater inflater;

    private ImageButton backBttn;
    private TextView locationTitle;

    /**
     * Set up new instance of browsing fragment with parent frame holder
     * @param parent int representing id of the frame layout this fragment is being put into
     * @return new instance of mapfragment with arguments available
     */
    public static BrowsingFragment newInstance(int parent, String locId, String locName) {
        BrowsingFragment bfrag = new BrowsingFragment();
        Bundle args = new Bundle();
        args.putInt("parent", parent);
        args.putString("lID", locId);
        args.putString("name", locName);
        bfrag.setArguments(args);
        return bfrag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Browsing:", "STARTING BROWSING LIST");
        super.onCreate(savedInstanceState);
        parentFrameHolder = getArguments().getInt("parent");
        locationId = getArguments().getString("lID");
        locationName = getArguments().getString("name");
        numDisplay = 0;
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
                FragmentManager fm = getActivity().getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                fm.popBackStackImmediate();
                ft.commit();
            }
        });

        locationTitle = (TextView)v.findViewById(R.id.location_title);
        locationTitle.setText(locationName);

        db = FirebaseDatabase.getInstance().getReference();
        pqueue = new PriorityQueue<MetaPage>(20, new MetaPage.MetaPageComparator());
        getLocationInformation(v);
    }

    /**
     * Extract Location Information pertaining to the POI passed in to this fragment
     * @param v View instance that this fragment inflates
     */
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
                Log.d("NUM:","Num Entries Gotten: " + pqueue.size());
                drawpList = new MetaPage[pqueue.size()];
                numDisplay = pqueue.size();
                pqueue.toArray(drawpList);
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
            return numDisplay;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            final View page = inflater.inflate(R.layout.browse_page, null);

            final MetaPage data = drawpList[position];
            //Get the Drawp
            Log.d("Processing:", data.getOwner() + " with total " + numDisplay);
            ImageView drawing = (ImageView)page.findViewById(R.id.drawing);
            Picasso.with(getActivity()).load(data.getImgUrl()).resize(500,500).into(drawing);
            TextView imgInfo = (TextView)page.findViewById(R.id.img_info);
            //Load the user's profile picture
            final ImageView userProf = (ImageView)page.findViewById(R.id.profile_picture);
            db.child("users").child(data.getOwner()).child("profile_url").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        Picasso.with(getContext()).load(dataSnapshot
                                .getValue(String.class)).resize(250,250).into(userProf);
                    }
                    else {
                        Picasso.with(getContext()).load(R.drawable.user_icon).resize(250,250).into(userProf);
                    }
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.e("Error:", databaseError.getMessage());
                }
            });

            ToggleButton upBttn = (ToggleButton) page.findViewById(R.id.upvote);
            TextView upNum = (TextView)page.findViewById(R.id.upvote_num);
            ToggleButton downBttn = (ToggleButton)page.findViewById(R.id.downvote);
            TextView downNum = (TextView)page.findViewById(R.id.downvote_num);
            upBttn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    //Setup
                    ToggleButton upBttn = (ToggleButton)page.findViewById(R.id.upvote);
                    ToggleButton downBttn = (ToggleButton)page.findViewById(R.id.downvote);
                    TextView upNum = (TextView) page.findViewById(R.id.upvote_num);
                    int currUp = data.getNumUpvotes();

                    //Button is now being checked
                    if(isChecked) {
                        //Set the View's value
                        upNum.setText(getString(R.string.general, String.valueOf(currUp + 1)));

                        //Update the db
                        db.child("locations").child(locationId).child(data.getOwner()).child("num_upvotes").setValue(currUp + 1);
                        db.child("users").child(data.getOwner()).child(locationId).child("num_upvotes").setValue(currUp + 1);

                        //Handle if the downvote button was previously chosen
                        if(downBttn.isChecked()) {
                            TextView downNum = (TextView) page.findViewById(R.id.downvote_num);
                            downBttn.setChecked(false);

                            int currDown = data.getNumDownvotes();
                            downNum.setText(getString(R.string.general, String.valueOf(currDown)));

                            db.child("locations").child(locationId).child(data.getOwner()).child("num_downvotes").setValue(currDown);
                            db.child("users").child(data.getOwner()).child(locationId).child("num_downvotes").setValue(currDown);
                        }
                    }
                    //Button is now being unchecked
                    else {
                        upNum.setText(getString(R.string.general, String.valueOf(currUp)));

                        //update the db
                        db.child("locations").child(locationId).child(data.getOwner()).child("num_upvotes").setValue(currUp);
                        db.child("users").child(data.getOwner()).child(locationId).child("num_upvotes").setValue(currUp);
                    }
                }
            });

            downBttn.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton button, boolean isChecked) {
                    //Setup
                    ToggleButton upBttn = (ToggleButton)page.findViewById(R.id.upvote);
                    ToggleButton downBttn = (ToggleButton)page.findViewById(R.id.downvote);
                    TextView downNum = (TextView) page.findViewById(R.id.downvote_num);
                    int currDown = data.getNumDownvotes();

                    //Button is now being checked
                    if(isChecked) {
                        downNum.setText(getString(R.string.general, String.valueOf(currDown + 1)));
                        //Update the db
                        db.child("locations").child(locationId).child(data.getOwner()).child("num_downvotes").setValue(currDown+1);
                        db.child("users").child(data.getOwner()).child(locationId).child("num_downvotes").setValue(currDown+1);

                        //Handle if the upvote button was previously chosen
                        if(upBttn.isChecked()) {
                            TextView upNum = (TextView)page.findViewById(R.id.upvote_num);
                            upBttn.setChecked(false);
                            int currUp = data.getNumUpvotes();
                            upNum.setText(getString(R.string.general, String.valueOf(currUp)));
                            //update db
                            db.child("locations").child(locationId).child(data.getOwner()).child("num_upvotes").setValue(currUp);
                            db.child("users").child(data.getOwner()).child(locationId).child("num_upvotes").setValue(currUp);
                        }
                    }
                    //Button is now being unchecked
                    else {
                        downBttn.setText(getString(R.string.general, String.valueOf(currDown)));
                        //update db
                        db.child("locations").child(locationId).child(data.getOwner()).child("num_downvotes").setValue(currDown);
                        db.child("users").child(data.getOwner()).child(locationId).child("num_downvotes").setValue(currDown);
                    }
                }
            });

            imgInfo.setText(getString(R.string.general, data.getOwner()));
            upNum.setText(String.valueOf(data.getNumUpvotes()));
            downNum.setText(String.valueOf(data.getNumDownvotes()));

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
