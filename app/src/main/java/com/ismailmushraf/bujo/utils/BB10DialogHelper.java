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

    public static void showAlertDialog(Context context, String title, String message, String btnText) {
        if (context == null) return;

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_bb10_confirm, null);
        TextView tvTitle = (TextView) view.findViewById(R.id.tv_confirm_title);
        TextView tvMessage = (TextView) view.findViewById(R.id.tv_confirm_message);
        TextView btnPositive = (TextView) view.findViewById(R.id.btn_confirm_positive);
        TextView btnNegative = (TextView) view.findViewById(R.id.btn_confirm_negative);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);
        
        // Use the negative button as the single 'OK' button because it's styled normally (black text), 
        // while the positive one is usually red (Delete). Actually, let's use positive and change color if needed, or just use negative.
        if (btnNegative != null) {
            btnNegative.setText(btnText != null ? btnText : "OK");
        }
        
        if (btnPositive != null) {
            btnPositive.setVisibility(View.GONE);
            // also hide the divider above negative button
            ViewGroup parent = (ViewGroup) btnPositive.getParent();
            if (parent != null && parent.getChildCount() > 1) {
                View div = parent.getChildAt(1); // divider is at index 1
                if (div != null) div.setVisibility(View.GONE);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BujoDialog);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

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

    public interface OnSidebarInflatedListener {
        void onSidebarInflated(android.app.Dialog dialog, View view);
    }

    public static android.app.Dialog showSidebar(Context context, int layoutResId, OnSidebarInflatedListener listener) {
        if (context == null) return null;

        final android.app.Dialog dialog = new android.app.Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        View view = LayoutInflater.from(context).inflate(layoutResId, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = android.view.WindowManager.LayoutParams.MATCH_PARENT;
            lp.gravity = android.view.Gravity.END;
            lp.windowAnimations = R.style.BB10SidebarAnimation;
            dialog.getWindow().setAttributes(lp);
        }

        View scrim = view.findViewById(R.id.sidebar_dim_scrim);
        if (scrim != null) {
            scrim.setOnClickListener(v -> dialog.dismiss());
        }

        if (listener != null) {
            listener.onSidebarInflated(dialog, view);
        }

        dialog.show();
        return dialog;
    }
}
