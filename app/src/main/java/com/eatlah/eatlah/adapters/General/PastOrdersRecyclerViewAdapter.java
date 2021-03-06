package com.eatlah.eatlah.adapters.General;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eatlah.eatlah.R;
import com.eatlah.eatlah.activities.Courier.CourierHomepage;
import com.eatlah.eatlah.activities.Customer.CustomerHomepage;
import com.eatlah.eatlah.adapters.Courier.CourierBasicOrderItemRecyclerViewAdapter;
import com.eatlah.eatlah.models.Order;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

public class PastOrdersRecyclerViewAdapter extends RecyclerView.Adapter<PastOrdersRecyclerViewAdapter.ViewHolder> {

    private final List<Order> mValues;
    private final Activity mListener;
    private String mAddress;

    public PastOrdersRecyclerViewAdapter(List<Order> items, Activity listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.general_view_holder_past_orders, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.item = mValues.get(position);
        holder.orderIdView.setText(mValues.get(position).getTimestamp());
        bindID(holder);
        holder.timeView.setText((mValues.get(position).getCollectionTime() != null)
                                ? mValues.get(position).getCollectionTime()
                                : "No time specified");
        holder.customerIdView.setText(holder.item.isTransaction_complete()
                                      ? "Transaction complete."
                                      : holder.item.isReady()
                                        ? holder.item.isCourierAttending()
                                          ? "Ready for courier pickup"
                                          : "Ready for pickup"
                                        : "Not ready yet");
        holder.orderItemsView.setLayoutManager(new LinearLayoutManager((Activity) mListener));
        holder.orderItemsView.setAdapter(new CourierBasicOrderItemRecyclerViewAdapter((Activity)mListener, holder.item.getOrders()));
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                respond(holder.item);
            }
        });

        String userId = mValues.get(position).getUser_id();
        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("address")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String address = (String) dataSnapshot.getValue();
                        holder.orderAdress_textView.setText(address);
                        mAddress = address;
                        System.out.println("THIS IS THE PLACE");
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
    }

    private void respond(Order selectedItem) {
        if (mListener instanceof CustomerHomepage) {
            ((CustomerHomepage)mListener).displayCustomerReceiptFragment(selectedItem);
        } else {
            ((CourierHomepage)mListener).displayCourierReceipt(selectedItem, mAddress);
        }
    }

    private void bindID(ViewHolder holder) {
        if (mListener instanceof CourierHomepage) {
            holder.customerIdView.setText(holder.item.getUser_id());
        } else if (mListener instanceof CustomerHomepage) {
            holder.customerIdView.setText(holder.item.getCourier_id());
        }
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderIdView;
        TextView customerIdView;
        TextView timeView;
        RecyclerView orderItemsView;
        Order item;
        TextView orderAdress_textView;

        public ViewHolder(View view) {
            super(view);
            orderIdView = view.findViewById(R.id.orderId_textView);
            orderAdress_textView = view.findViewById(R.id.orderAdress_textView);
            customerIdView = view.findViewById(R.id.customerId_textView);
            timeView = view.findViewById(R.id.orderTime_textView);
            orderItemsView = view.findViewById(R.id.orderItem_recyclerView);
        }
    }
}
