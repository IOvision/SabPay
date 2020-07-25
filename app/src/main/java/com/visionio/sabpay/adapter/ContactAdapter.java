package com.visionio.sabpay.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.visionio.sabpay.models.Contact;
import com.visionio.sabpay.R;
import com.visionio.sabpay.interfaces.OnItemClickListener;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    Context context;

    List<Contact> filteredContactList;
    List<Contact> completeContactList;

    OnItemClickListener<Contact> clickListener;


    public ContactAdapter(Context context, List<Contact> filteredContactList, List<Contact> completeContactList) {
        this.context = context;
        this.filteredContactList = filteredContactList;
        this.completeContactList = completeContactList;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_list_item, parent, false);
        return new ContactViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        holder.name.setText(filteredContactList.get(position).getName());
        holder.number.setText(filteredContactList.get(position).getNumber());
        holder.bind(filteredContactList.get(position),position, clickListener);
        filteredContactList.get(position).setSelector(holder.selector);
        filteredContactList.get(position).positionInAdapter = position;

    }

    @Override
    public int getItemCount() {
        return filteredContactList.size();
    }

    public void add(Contact contact){
        completeContactList.add(contact);
        filteredContactList.add(contact);

        if(filteredContactList.size()>0){
            Collections.sort(filteredContactList, new Comparator<Contact>() {
                @Override
                public int compare(Contact o1, Contact o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
        }

        notifyDataSetChanged();
    }

    public void applyFilter(String filterString){
        filteredContactList.clear();
        for(Contact c: completeContactList){
            if(c.getNumber().contains(filterString) || c.getName().toLowerCase().contains(filterString)){
                filteredContactList.add(c);
            }
        }
        notifyDataSetChanged();
    }

    public void setClickListener(OnItemClickListener<Contact> clickListener) {
        this.clickListener = clickListener;
    }

    public void unSelect(int pos){
        filteredContactList.get(pos).getSelector().setChecked(false);
        notifyDataSetChanged();
    }

    public void select(int pos){
        filteredContactList.get(pos).getSelector().setChecked(true);
        notifyDataSetChanged();
    }

    public void addUserToContact(Contact c){
        for(Contact contact: filteredContactList){
            if(contact.getUser()==null && c.getUser()!=null && contact.getNumber().equals(c.getNumber())){
                contact.setUser(c.getUser());
            }
        }
    }


    public List<Contact> getFilteredContactList(){
        return filteredContactList;
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder{

        TextView name;
        TextView number;
        View itemView;

        RadioButton selector;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            name = itemView.findViewById(R.id.contact_item_name_tv);
            number = itemView.findViewById(R.id.contact_item_number_tv);
            selector = itemView.findViewById(R.id.contact_item_selector_rb);
        }

        public void bind(final Contact contact, final int position, final OnItemClickListener listener){
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(contact, position, v);
                }
            });
            selector.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClicked(contact, position, v);
                }
            });
        }

    }
}
