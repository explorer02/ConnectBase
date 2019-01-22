package com.example.connectbase;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;


public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    ArrayList<String>userKeys=new ArrayList<>();
    ArrayList<Users>userList=new ArrayList<>();



    interface onclickItem{

        void onItemClicked(int i);
    }
    onclickItem activity;

    public UserAdapter(Context context,ArrayList<Users>users,ArrayList<String>keys){
        activity=(onclickItem)context;
        userKeys=keys;
        userList=users;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        TextView tvName,tvPosition;
        CircleImageView ivPhoto;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName=itemView.findViewById(R.id.tv_layoutRowPeople_name);
            tvPosition=itemView.findViewById(R.id.tv_layoutRowPeople_position);
            ivPhoto=itemView.findViewById(R.id.iv_layoutRowPeople_profilePic);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

        View view=LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_people,viewGroup,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, final int i) {

        viewHolder.tvName.setText(userList.get(i).getName());
        viewHolder.tvPosition.setText(userList.get(i).getPosition());

        if(!userList.get(i).getThumbImage().isEmpty())
        Glide.with(viewHolder.ivPhoto.getContext())
                .load(userList.get(i).getThumbImage())
                .into(viewHolder.ivPhoto);

        viewHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    activity.onItemClicked(i);
            }
        });

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


}
