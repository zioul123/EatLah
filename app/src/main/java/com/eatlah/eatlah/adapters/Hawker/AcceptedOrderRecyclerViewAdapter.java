package com.eatlah.eatlah.adapters.Hawker;

import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eatlah.eatlah.R;
import com.eatlah.eatlah.activities.Hawker.HawkerHomepage;
import com.eatlah.eatlah.fragments.Hawker.AcceptedOrderFragment;
import com.eatlah.eatlah.fragments.Hawker.AcceptedOrderFragment.OnListFragmentInteractionListener;
import com.eatlah.eatlah.models.Order;
import com.eatlah.eatlah.models.OrderItem;
import com.eatlah.eatlah.models.User;

import java.util.Iterator;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link OrderItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class AcceptedOrderRecyclerViewAdapter extends RecyclerView.Adapter<AcceptedOrderRecyclerViewAdapter.ViewHolder> {
    private static int GREEN = 0xFF00FA9A;
    private static int RED = 0xFFFFC0CB;
    private static int CYAN = 0xFF6BD6D6;
    private static int SAND = 0xFFEDDEA4;
    private static int SUNSET = 0xFFF7A072;

    private final List<Order> mValues;
    private final OnListFragmentInteractionListener mListener;

    public AcceptedOrderRecyclerViewAdapter(List<Order> items, OnListFragmentInteractionListener listener) {
        mValues = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.hawker_view_holder_acceptedorder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final Order order = mValues.get(position);
        holder.mName.setText(order.getCollectionTime());
        holder.mDesc.setText(order.isReady()
                             ? "Ready!"
                             : isItReadyForThisHawker(order)
                               ? "Ready on your side"
                               : "Not ready.");

        // Calculate price and set text
        String hawkerId = AcceptedOrderFragment.user.get_hawkerId();
        calculatePrice(order, holder.mPrice, hawkerId);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onListFragmentInteraction(order);
            }
        });

        // Color based on "isReady"
        holder  .mCardView
                .setCardBackgroundColor(
                        order.isReady()
                        ? CYAN
                        : isItReadyForThisHawker(order)
                          ? SAND
                          : SUNSET);
    }

    private boolean isItReadyForThisHawker(Order order) {
        User user = HawkerHomepage.mUser;
        List<OrderItem> orders = order.getOrders();

        boolean result = true; Iterator<OrderItem> iter = orders.iterator();

        // Loop through all, if any belong to him and are incomplete, false.
        while (result && iter.hasNext()) {
            OrderItem o = iter.next();
            if (o.getStall_id().equals(user.get_hawkerId()) && !o.isComplete())
                result = false;

        }
        return result;
    }

    private void calculatePrice(Order order, TextView priceView, String hawkerId) {
        double price = 0;
        for (OrderItem orderItem : order.getOrders()) {
            if (orderItem.getStall_id().equals(hawkerId))
                price += orderItem.getQty() * Double.parseDouble(orderItem.getPrice());
        }
        priceView.setText(Double.toString(price));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView mName;
        private TextView mPrice;
        private TextView mDesc;
        private CardView mCardView;

        public ViewHolder(View view) {
            super(view);
            mName   = view.findViewById(R.id.acceptedorder_orderName_textView);
            mPrice  = view.findViewById(R.id.acceptedorder_price_textView);
            mDesc   = view.findViewById(R.id.acceptedorder_orderDesc_textView);
            mCardView = view.findViewById(R.id.acceptedorder_hc_view_holder);
        }
    }
}
