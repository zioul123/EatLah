package com.eatlah.eatlah.fragments.Customer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.eatlah.eatlah.R;
import com.eatlah.eatlah.adapters.Courier.CourierBasicOrderItemRecyclerViewAdapter;
import com.eatlah.eatlah.helpers.QRCodeGenerator;
import com.eatlah.eatlah.models.Order;
import com.eatlah.eatlah.models.OrderItem;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.util.concurrent.ExecutionException;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CustomerReceiptFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CustomerReceiptFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CustomerReceiptFragment extends Fragment {
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String TAG = CustomerReceiptFragment.class.getSimpleName();
    private static final String ORDER_TAG = "order";
    private static final String CUSTOMER_ADDRESS_TAG = "customerAddress";

    private Order order;
    private String customerAddress;
    private QRCodeGenerator qrCodeGenerator;

    private CourierBasicOrderItemRecyclerViewAdapter mAdapter;
    private CustomerReceiptFragment.OnFragmentInteractionListener mListener;

    private Button completedOrder_button;

    public CustomerReceiptFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param order
     * @param customerAddress
     * @return A new instance of fragment CourierReceiptFragment.
     */
    public static CustomerReceiptFragment newInstance(Order order, String customerAddress) {
        CustomerReceiptFragment fragment = new CustomerReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(ORDER_TAG, order);
        args.putString(CUSTOMER_ADDRESS_TAG, customerAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.order = (Order) getArguments().getSerializable(ORDER_TAG);
            this.customerAddress = getArguments().getString(CUSTOMER_ADDRESS_TAG);
            this.qrCodeGenerator = new QRCodeGenerator(order.getTimestamp());
            qrCodeGenerator.execute();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.CUPCAKE)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View fragmentView = inflater.inflate(R.layout.customer_fragment_receipt, container, false);
        ((TextView) fragmentView.findViewById(R.id.customerId_textView)).setText(order.getUser_id());
        ((TextView) fragmentView.findViewById(R.id.customerAddress_textView)).setText(customerAddress);
        TextView subtotal_textView = fragmentView.findViewById(R.id.amtToCollect_textView);
        setSubtotal(subtotal_textView);

        completedOrder_button = fragmentView.findViewById(R.id.completedOrder_button);
        displayRelevantFields(fragmentView);

        RecyclerView orderItemsRecyclerView = fragmentView.findViewById(R.id.orderItems_recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager((Activity) mListener);
        orderItemsRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new CourierBasicOrderItemRecyclerViewAdapter((Activity)mListener, order.getOrders());
        orderItemsRecyclerView.setAdapter(mAdapter);

        //address code
        String userId = order.getUser_id();
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("address")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String address = (String) dataSnapshot.getValue();
                        ((TextView) fragmentView.findViewById(R.id.customerAddress_textView)).setText(address);
                        System.out.println("Customer address set");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });


        return fragmentView;
    }

    private void displayRelevantFields(View fragmentView) {
        if (order.isSelf_collection()) {
            Log.d(TAG, "displaying fields for self collection");
            displayFieldsForSelfCollection(fragmentView);
        } else {
            Log.d(TAG, "displaying fields for courier delivery");
            displayFieldsForCourierDelivery(fragmentView);
        }
    }

    private void displayFieldsForCourierDelivery(View fragmentView) {
        completedOrder_button.setVisibility(View.GONE);
        // qr code generated on customerReceiptView to be scanned by courier
        // upon scanning qr code, order marked as complete
        getQRCode(fragmentView);
    }

    private void displayFieldsForSelfCollection(View fragmentView) {
        fragmentView.findViewById(R.id.qrCode_imageView)
                .setVisibility(View.GONE);

        completedOrder_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onOrderCompletion();
            }
        });
    }

    private void getQRCode(View fragmentView) {
        try {
            Log.d(TAG, "getting qr code from generator");
            Bitmap qrCode = qrCodeGenerator.get();
            ((ImageView) fragmentView.findViewById(R.id.qrCode_imageView))
                    .setImageBitmap(qrCode);
            Log.d(TAG, "set imageview as qrcode");
        } catch (InterruptedException e) {
            Log.e(TAG, e.getLocalizedMessage());
        } catch (ExecutionException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
    }

    private void setSubtotal(TextView subtotal_textView) {
        double cost = 0;
        for (OrderItem orderItem : order.getOrders()) {
            cost += (Double.parseDouble(orderItem.getPrice()) * orderItem.getQty());
        }
        DecimalFormat df = new DecimalFormat("##.00");
        subtotal_textView.setText(String.format("$%s", df.format(cost)));
    }

    public void onOrderCompletion() {
        mListener.onFragmentInteraction(order);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof CustomerReceiptFragment.OnFragmentInteractionListener) {
            mListener = (CustomerReceiptFragment.OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
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
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Order order);
    }
}
