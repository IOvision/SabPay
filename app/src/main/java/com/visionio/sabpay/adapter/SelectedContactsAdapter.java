package com.visionio.sabpay.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.Models.Contact;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.List;

public class SelectedContactsAdapter extends RecyclerView.Adapter<SelectedContactsAdapter.SelectedContactsViewHolder> {


    int type=0; // 0 for displaying small cross in corner and one only for displaying
    List<Contact> contacts;
    OnItemClickListener<Contact> clickListener;


    public SelectedContactsAdapter(List<Contact> contacts) {
        this.contacts = contacts;
    }

    public void setClickListener(OnItemClickListener<Contact> clickListener) {
        this.clickListener = clickListener;
    }

    public void setType(int type1){
        type = type1;
    }

    public List<Contact> getContacts() {
        return contacts;
    }

    @NonNull
    @Override
    public SelectedContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.selected_contact_list_item, parent, false);
        return new SelectedContactsViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull final SelectedContactsViewHolder holder, final int position) {
        final Contact current = contacts.get(position);
        holder.initials.setText(""+current.getName().charAt(0));
        holder.name.setText(current.getName());

        if(type==0){
            holder.cancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    clickListener.onItemClicked(current, position, holder.view);

                }
            });

            holder.view.animate().scaleX(1).scaleY(1).setInterpolator(new OvershootInterpolator()).setDuration(437).start();
        }else{
            holder.view.animate().scaleX(1).scaleY(1).setInterpolator(new AccelerateInterpolator()).setDuration(100).start();
            holder.cancel.setVisibility(View.GONE);
        }


    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public void add(Contact contact){
        if(!contacts.contains(contact)){
            contacts.add(contact);
            notifyDataSetChanged();
        }
    }

    public void remove(int pos){
        contacts.remove(pos);
        notifyItemRemoved(pos);
    }

    public void remove(Contact contact){
        contacts.remove(contact);
        notifyDataSetChanged();
    }

    public class SelectedContactsViewHolder extends RecyclerView.ViewHolder{

        ImageView cancel;
        TextView initials;
        TextView name;
        View view;

        public SelectedContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            cancel = itemView.findViewById(R.id.selected_contact_cancel_iv);
            initials = itemView.findViewById(R.id.selected_contact_initials_tv);
            name = itemView.findViewById(R.id.selected_contact_name_tv);
            view = itemView;
        }

    }

}
