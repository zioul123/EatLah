package com.eatlah.eatlah.fragments.Courier;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.eatlah.eatlah.R;
import com.eatlah.eatlah.adapters.Courier.CourierBasicOrderItemRecyclerViewAdapter;
import com.eatlah.eatlah.helpers.QRCodeDecoderActivity;
import com.eatlah.eatlah.helpers.barcode.BarcodeCaptureActivity;
import com.eatlah.eatlah.models.Order;
import com.eatlah.eatlah.models.OrderItem;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.text.DecimalFormat;

import static android.support.constraint.Constraints.TAG;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * Use the {@link CourierReceiptFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CourierReceiptFragment extends Fragment {
    private int BARCODE_READER_REQUEST_CODE = 1;

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ORDER_TAG = "order";
    private static final String CUSTOMER_ADDRESS_TAG = "customerAddress";

    private Order order;
    private String customerAddress;

    private CourierBasicOrderItemRecyclerViewAdapter mAdapter;
    private OnFragmentInteractionListener mListener;

    private Button completedOrder_button;

    public CourierReceiptFragment() {
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
    public static CourierReceiptFragment newInstance(Serializable order, String customerAddress) {
        CourierReceiptFragment fragment = new CourierReceiptFragment();
        Bundle args = new Bundle();
        args.putSerializable(ORDER_TAG, order);
        args.putString(CUSTOMER_ADDRESS_TAG, customerAddress);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.order = (Order) getArguments().getSerializable(ORDER_TAG);
            this.customerAddress = getArguments().getString(CUSTOMER_ADDRESS_TAG);
            System.out.println("On create fragment, order : " + order );
        }
    }

    /**
     * starts qr code scanner activity
     */
    private void scanQRCode() {
        //startActivityForResult(new Intent((Activity) mListener, QRCodeDecoderActivity.class), QRCodeDecoderActivity.QRCODE_DECODER_REQUEST_CODE);

        Intent intent = new Intent((Activity) mListener, BarcodeCaptureActivity.class);
        startActivityForResult(intent, BARCODE_READER_REQUEST_CODE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BARCODE_READER_REQUEST_CODE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    final Barcode qr = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    Toast.makeText(getContext(), qr.displayValue, Toast.LENGTH_SHORT).show();
                    System.out.println(qr.displayValue);
                    FirebaseDatabase dB = FirebaseDatabase.getInstance();
                    final DatabaseReference orderRef = dB.getReference("Orders")
                                                    .child(qr.displayValue);
                    orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            Order o = dataSnapshot.getValue(Order.class);
                            o.setTransaction_complete(true);
                            orderRef.setValue(o);
                            Toast.makeText(getContext(), "Transaction" + qr.displayValue + " marked as complete!", Toast.LENGTH_SHORT).show();

                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                } else {
                    System.out.println("No QR code read.");
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.courier_fragment_receipt, container, false);
        ((TextView) fragmentView.findViewById(R.id.courierId_textView)).setText(order.getCourier_id());
        ((TextView) fragmentView.findViewById(R.id.customerId_textView)).setText(order.getUser_id());
        ((TextView) fragmentView.findViewById(R.id.customerAddress_textView)).setText(customerAddress);
        TextView subtotal_textView = fragmentView.findViewById(R.id.amtToCollect_textView);
        setSubtotal(subtotal_textView);

        completedOrder_button = fragmentView.findViewById(R.id.completedOrder_button);
        completedOrder_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // after qrcode is scanned
                // order is marked as complete
                Log.d(TAG, "scanning qr code");
                scanQRCode();
            }
        });

        RecyclerView orderItemsRecyclerView = fragmentView.findViewById(R.id.orderItems_recyclerView);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager((Activity) mListener);
        orderItemsRecyclerView.setLayoutManager(layoutManager);
        mAdapter = new CourierBasicOrderItemRecyclerViewAdapter((Activity)mListener, order.getOrders());
        orderItemsRecyclerView.setAdapter(mAdapter);
        return fragmentView;
    }

    private void setSubtotal(TextView subtotal_textView) {
        double cost = 0;
        for (OrderItem orderItem : order.getOrders()) {
            cost += (Double.parseDouble(orderItem.getPrice()) * orderItem.getQty());
        }
        DecimalFormat df = new DecimalFormat("##.00");
        subtotal_textView.setText(String.format("$%s", df.format(cost)));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
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
        void onFragmentInteraction(Order order, Boolean overloader);
    }
}
