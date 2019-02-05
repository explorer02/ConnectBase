package com.example.connectbase;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

public class ViewFilesActivity extends AppCompatActivity {

    ArrayList<String> pathList = new ArrayList<>();
    ArrayList<FileModel> fileModelArrayList = new ArrayList<>();
    RecyclerView fileList;
    Adapter adapter = new Adapter();
    FloatingActionButton fabDoneEditing;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_files);

        fabDoneEditing = findViewById(R.id.fab_viewFiles_done);
        pathList = getIntent().getBundleExtra("bundle").getStringArrayList("pathList");
        fileList = findViewById(R.id.list_viewFiles);
        fileList.setLayoutManager(new LinearLayoutManager(this));
        fileList.setHasFixedSize(true);
        fileList.setAdapter(adapter);
        fileList.setItemAnimator(new DefaultItemAnimator());
        fileList.swapAdapter(adapter, true);
        new LoadFiles().execute();

        fabDoneEditing.setOnClickListener(v -> doneEditing(v));

    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Go Back??")
                .setMessage("Are you sure you do not want to send these Files?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", (dialog, which) -> {
                    for (int i = 0; i < pathList.size(); i++)
                        new File(pathList.get(i)).delete();
                    dialog.dismiss();
                    Intent intent = new Intent(ViewFilesActivity.this, ChatActivity.class);
                    setResult(RESULT_CANCELED, intent);
                    finish();
                });
        builder.show();
    }

    public void doneEditing(View view) {
        Intent intent = new Intent(this, ChatActivity.class);
        setResult(RESULT_OK, intent);
        Bundle bundle = new Bundle();
        ArrayList<String> descList = new ArrayList<>();

        for (int i = 0; i < fileModelArrayList.size(); i++)
            descList.add(fileModelArrayList.get(i).getDesc().substring(12));
        bundle.putStringArrayList("descList", descList);
        bundle.putStringArrayList("pathList", pathList);
        intent.putExtra("bundle", bundle);
        finish();

    }

    public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.layout_row_view_file, viewGroup, false);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {

            viewHolder.tvType.setText(fileModelArrayList.get(i).getType());
            viewHolder.tvDesc.setText(fileModelArrayList.get(i).getDesc());
            viewHolder.tvName.setText(fileModelArrayList.get(i).getName());
            viewHolder.tvSize.setText(fileModelArrayList.get(i).getSize());

            int pos = viewHolder.getAdapterPosition();

            viewHolder.ivDelete.setOnClickListener(v -> {
                File file = new File(pathList.get(pos));
                if (file.exists()) ;
                file.delete();
                pathList.remove(pos);
                fileModelArrayList.remove(pos);
                notifyItemRemoved(pos);
                notifyItemRangeChanged(pos, getItemCount());
            });

            viewHolder.ivEdit.setOnClickListener(v -> {

                AlertDialog.Builder builder = new AlertDialog.Builder(ViewFilesActivity.this);
                builder.setTitle("Add Description");

                ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                LinearLayout linearLayout = new LinearLayout(ViewFilesActivity.this);
                linearLayout.setLayoutParams(params);
                linearLayout.setPadding(20, 20, 20, 10);
                EditText editText = new EditText(ViewFilesActivity.this);

                linearLayout.addView(editText, params);
                builder.setView(linearLayout);
                editText.setHint("Type Something..");
                editText.setText(fileModelArrayList.get(pos).getDesc().trim());
                editText.setInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
                builder.setCancelable(false);
                builder.setNegativeButton("Cancel", null);
                builder.setPositiveButton("Ok", (dialog1, which) -> {
                    String text = editText.getText().toString().trim();
                    fileModelArrayList.get(pos).setDesc("Description: " + text);
                    notifyItemChanged(pos);
                });
                builder.show();
            });


        }

        @Override
        public int getItemCount() {
            return fileModelArrayList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView tvName, tvSize, tvDesc, tvType;
            ImageView ivDelete, ivEdit;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_lRVF_name);
                tvDesc = itemView.findViewById(R.id.tv_lRVF_desc);
                tvSize = itemView.findViewById(R.id.tv_lRVF_size);
                tvType = itemView.findViewById(R.id.tv_lRVF_type);
                ivDelete = itemView.findViewById(R.id.iv_lRVF_delete);
                ivEdit = itemView.findViewById(R.id.iv_lRVF_edit);
            }
        }
    }

    class FileModel {

        private String name, size, type, desc;

        public FileModel(String name, String size, String type, String desc) {
            this.name = name;
            this.size = size;
            this.type = type;
            this.desc = desc;
        }

        public String getName() {
            return name;
        }

        public String getSize() {
            return size;
        }


        public String getType() {
            return type;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }
    }

    public class LoadFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < pathList.size(); i++) {
                File file = new File(pathList.get(i));
                String name = file.getName();

                //TODO: Check File name

                if (name.contains("."))
                    name = name.substring(0, name.lastIndexOf("."));

                long fileSize = file.length();
                String size = "";
                fileSize /= 1024;
                if (fileSize < 1024)
                    size = "Size: " + fileSize + " KB";
                if (fileSize > 1024)
                    size = "Size: " + fileSize / 1024 + " MB";

                String type = "Type: " + file.getName().substring(file.getName().lastIndexOf(".") + 1);
                String desc = "Description:";

                fileModelArrayList.add(new FileModel(name, size, type, desc));
                publishProgress();

            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            adapter.notifyDataSetChanged();

        }

    }

}
