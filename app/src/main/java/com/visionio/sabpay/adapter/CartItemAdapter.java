package com.visionio.sabpay.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;
import com.visionio.sabpay.models.Item;
import com.visionio.sabpay.models.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import it.sephiroth.android.library.numberpicker.NumberPicker;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {

    List<Item> itemList;
    Context context;

    OnItemClickListener<Item> clickListener;

    public CartItemAdapter(List<Item> itemList, Context context, OnItemClickListener<Item> clickListener) {
        this.itemList = itemList;
        this.context = context;
        this.clickListener = clickListener;
    }

    public CartItemAdapter(List<Item> itemList, Context context) {
        this.itemList = itemList;
        this.context = context;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }


    public void setClickListener(OnItemClickListener<Item> clickListener) {
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item, parent, false);
        return new CartItemViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        Item curr = itemList.get(position);

        holder.detail_tv.setText(String.format("%s ₹%s/%s", curr.getTitle(), curr.getCost(), curr.getUnit()));
        holder.qty_np.setMaxValue(curr.getQty());

        holder.qty_np.setMinValue(0);
        holder.qty_np.setMaxValue(curr.getQty());
        holder.qty_np.setProgress(curr.getCart_qty());

        holder.qty_np.setNumberPickerChangeListener(new NumberPicker.OnNumberPickerChangeListener() {
            @Override
            public void onProgressChanged(@NotNull NumberPicker numberPicker, int i, boolean b) {
                Utils.toast(context, "prog: "+i, Toast.LENGTH_SHORT);
                curr.setCart_qty(i);

                if(i==0 && getItemCount()>0){
                    itemList.remove(i);
                    notifyItemRemoved(position);
                    clickListener.onItemClicked(null, -2, null);
                }
                if(getItemCount()==0 && clickListener!=null){
                    // info: callback when all items from cart is removed
                    holder.qty_np.setNumberPickerChangeListener(null);
                    clickListener.onItemClicked(null, -1, null);
                }
            }

            @Override
            public void onStartTrackingTouch(@NotNull NumberPicker numberPicker) {
                Log.i("test", "onStartTrackingTouch: ");
            }

            @Override
            public void onStopTrackingTouch(@NotNull NumberPicker numberPicker) {
                Log.i("test", "onStopTrackingTouch: "+numberPicker.getProgress());
            }
        });

    }

    @Override
    public int getItemCount() {
        return itemList.size();
    }

    public List<Item> getItemList() {
        return new ArrayList<>(itemList);
    }

    class CartItemViewHolder extends RecyclerView.ViewHolder{

        TextView detail_tv;
        it.sephiroth.android.library.numberpicker.NumberPicker qty_np;

        View v;

        public CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            v = itemView;
            detail_tv = itemView.findViewById(R.id.cart_item_detail_tv);
            qty_np = itemView.findViewById(R.id.cart_item_numberPicker);
        }
    }
}
