package com.eatlah.eatlah.fragments.Courier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.eatlah.eatlah.R;
import com.eatlah.eatlah.activities.Courier.CourierMapsActivity;
import com.eatlah.eatlah.adapters.Courier.CourierPendingOrderRecyclerViewAdapter;
import com.eatlah.eatlah.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p/>
 * Activities containing this fragment MUST implement the {@link OnListFragmentInteractionListener}
 * interface.
 */
public class CourierPendingOrderFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    public static final int COMPLETED_ORDER = 1;
    private int mColumnCount = 1;

    private FirebaseDatabase mDb;
    private ArrayList<Order> mOrders;
    private CourierPendingOrderRecyclerViewAdapter mAdapter;
    private OnListFragmentInteractionListener mListener;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CourierPendingOrderFragment() {
        mOrders = new ArrayList<>();
        mDb = FirebaseDatabase.getInstance();
    }

    @SuppressWarnings("unused")
    public static CourierPendingOrderFragment newInstance(int columnCount) {
        CourierPendingOrderFragment fragment = new CourierPendingOrderFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_COLUMN_COUNT, columnCount);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.courier_fragment_pending_order_list, container, false);
        initMapViewButton((Activity) mListener);

        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            RecyclerView recyclerView = (RecyclerView) view;
            if (mColumnCount <= 1) {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
            } else {
                recyclerView.setLayoutManager(new GridLayoutManager(context, mColumnCount));
            }
            mAdapter = new CourierPendingOrderRecyclerViewAdapter(mOrders, mListener);
            recyclerView.setAdapter(mAdapter);
        }
        return view;
    }

    private void initMapViewButton(Activity activity) {
        FloatingActionButton fab = activity.findViewById(R.id.fab);
        fab.show();
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Switching to map view", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                // start map view
                displayMap();
            }
        });
    }

    /**
     * displays map activity with pins marking pending orders
     */
    private void displayMap() {
        Intent intent = new Intent(getActivity(), CourierMapsActivity.class);
        intent.putExtra(getResources().getString(R.string.pendingOrders), mOrders);
        startActivity(intent);
    }

    @Override
    public void onStart() {
        super.onStart();
        // retrieve orders from db
        retrieveOrders();
    }

    /**
     * retrieves the updated orders from db
     */
    private void retrieveOrders() {
        mDb.getReference(getResources().getString(R.string.order_ref))
                .orderByChild(getResources().getString(R.string.collectionTimeRef))   // order by collectiontime
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        // empty order list
                        mOrders.clear();
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Order order = snapshot.getValue(Order.class);

                            // only add order if order is for delivery and no courier has attended yet
                            if (!order.isSelf_collection() && !order.isCourierAttending()) {
                                System.out.println("order from db: " + order.getTimestamp());
                                mOrders.add(order);
                            }
                        }

                        updateOrderView();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("courier", databaseError.getMessage());
                    }

                });
    }

    /**
     * updates the order view on the UI thread
     */
    private void updateOrderView() {
        ((Activity)mListener).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnListFragmentInteractionListener) {
            mListener = (OnListFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnListFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnListFragmentInteractionListener {
        void onListFragmentInteraction(Order item);
    }
}
