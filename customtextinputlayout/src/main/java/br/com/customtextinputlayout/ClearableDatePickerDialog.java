package br.com.customtextinputlayout;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

public class ClearableDatePickerDialog extends DatePickerDialog {

    private OnDateClearedListener mOnDateClearedListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle state) {
        View view = super.onCreateView(inflater, container, state);
        LinearLayout buttonContainer = view.findViewById(
                com.wdullaer.materialdatetimepicker.R.id.mdtp_done_background);
        View clearButton = inflater.inflate(R.layout.date_picker_dialog_clear_button,
                buttonContainer, false);
        clearButton.setOnClickListener(new ClearClickListener());
        buttonContainer.addView(clearButton, 0);

        return view;
    }

    public void setOnDateClearedListener(OnDateClearedListener listener) {
        mOnDateClearedListener = listener;
    }

    public OnDateClearedListener getOnDateClearedListener() {
        return mOnDateClearedListener;
    }

    public interface OnDateClearedListener {
        /**
         * @param view The view associated with this listener.
         */
        void onDateCleared(ClearableDatePickerDialog view);
    }

    private class ClearClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            tryVibrate();

            OnDateClearedListener listener = getOnDateClearedListener();
            if (listener != null) {
                listener.onDateCleared(ClearableDatePickerDialog.this);
            }

            dismiss();
        }

    }

}