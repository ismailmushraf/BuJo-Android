package com.ismailmushraf.bujo.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ismailmushraf.bujo.R;

public class BB10DialogHelper {

    public static void showConfirmDialog(Context context, String title, String message, String positiveBtnText, Runnable onPositiveAction) {
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bb10_confirm, null);
        TextView tvTitle = (TextView) view.findViewById(R.id.tv_confirm_title);
        TextView tvMessage = (TextView) view.findViewById(R.id.tv_confirm_message);
        TextView btnPositive = (TextView) view.findViewById(R.id.btn_confirm_positive);
        TextView btnNegative = (TextView) view.findViewById(R.id.btn_confirm_negative);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
        if (btnPositive != null && positiveBtnText != null && !positiveBtnText.isEmpty()) {
            btnPositive.setText(positiveBtnText);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BujoDialog);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        if (btnPositive != null) {
            btnPositive.setOnClickListener(v -> {
                dialog.dismiss();
                if (onPositiveAction != null) {
                    onPositiveAction.run();
                }
            });
        }

        if (btnNegative != null) {
            btnNegative.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.88);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }
}
