package br.com.customtextinputlayout;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.date.DateRangeLimiter;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CustomTextInputLayout
        extends TextInputLayout {

    /**
     * Constante utilizada na definição como é a formatação brasileira de data & hora, moeda e casas decimais
     */
    public static final Locale LOCALE_BR = new Locale("pt", "BR");
    public static final DateFormat BRAZIL_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", LOCALE_BR);

    private TextInputEditText editText;
    private CustomAppCompatAutoCompleteTextView customSpinner;

    private boolean isSpinner = false;

    private float textSize;
    private ColorStateList textColor;
    private boolean enabled = true;
    private int textAlignment;
    private int textStyle;
    private int maxLength = 0;
    private boolean singleLine;
    private boolean specialChars = true;
    private int inputType;
    private Drawable endIconDrawable = null;
    private int endIconTint = -1;
    private int lines = 0;
    private int textGravity = 0;

    private int maskType = 0;

    private int decimalDigits = 2;
    private int divisor = 100;

    private boolean disableClearButton = false;

    private TextWatcher maskWatcher;
    private boolean clearFlag;

    private View.OnFocusChangeListener mOnFocusChangeListener;

    private Calendar maxDate;
    private Calendar minDate;
    private Calendar dataEscolhida;
    private boolean exibeDiaSemana = false;
    private boolean weekends = true;

    private Context context;

    public CustomTextInputLayout(Context context) {
        super(context);
        this.context = context;

        initConfig();
    }

    @SuppressLint("PrivateResource")
    public CustomTextInputLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.context = context;

        @SuppressLint("CustomViewStyleable") TypedArray c = context.obtainStyledAttributes(attrs,
                R.styleable.TextInputLayout);
        final int N1 = c.getIndexCount();
        for (int i = 0; i < N1; ++i) {
            int attr = c.getIndex(i);
            if (attr == R.styleable.TextInputLayout_endIconDrawable) {
                endIconDrawable = c.getDrawable(attr);
            } else if (attr == R.styleable.TextInputLayout_endIconTint) {
                endIconTint = c.getColor(attr, -1);
            }
        }
        c.recycle();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomTextInputLayout);
        final int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.CustomTextInputLayout_setSpinner) {
                isSpinner = a.getBoolean(attr, false);
            } else if (attr == R.styleable.CustomTextInputLayout_android_textSize) {
                textSize = a.getDimension(attr, 0);
            } else if (attr == R.styleable.CustomTextInputLayout_android_textColor) {
                textColor = a.getColorStateList(attr);
            } else if (attr == R.styleable.CustomTextInputLayout_android_enabled) {
                enabled = a.getBoolean(attr, true);
            } else if (attr == R.styleable.CustomTextInputLayout_android_textAlignment) {
                if (Build.VERSION.SDK_INT >= 19) {
                    textAlignment = a.getInt(attr, -1);
                } else if (Build.VERSION.SDK_INT >= 17) {
                    textAlignment = TEXT_ALIGNMENT_CENTER;
                }
            } else if (attr == R.styleable.CustomTextInputLayout_android_textStyle) {
                textStyle = a.getInt(attr, -1);
            } else if (attr == R.styleable.CustomTextInputLayout_textMaxLength) {
                maxLength = a.getInteger(attr, 0);
            } else if (attr == R.styleable.CustomTextInputLayout_isSingleLine) {
                singleLine = a.getBoolean(attr, false);
            } else if (attr == R.styleable.CustomTextInputLayout_enableSpecialChars) {
                specialChars = a.getBoolean(attr, true);
            } else if (attr == R.styleable.CustomTextInputLayout_android_inputType) {
                inputType = a.getInt(attr, EditorInfo.TYPE_TEXT_VARIATION_NORMAL);
            } else if (attr == R.styleable.CustomTextInputLayout_setLines) {
                lines = a.getInteger(attr, 0);
            } else if (attr == R.styleable.CustomTextInputLayout_maskType) {
                maskType = a.getInt(attr, 0);
            } else if (attr == R.styleable.CustomTextInputLayout_setDecimalDigits) {
                decimalDigits = a.getInt(attr, 2);
            } else if (attr == R.styleable.CustomTextInputLayout_disableClearButton) {
                disableClearButton = a.getBoolean(attr, false);
            } else if (attr == R.styleable.CustomTextInputLayout_textGravity) {
                textGravity = a.getInt(attr, Gravity.NO_GRAVITY);
            }
        }

        a.recycle();
        initConfig();
    }

    public CustomTextInputLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.context = context;

        initConfig();
    }

    private void initConfig() {
        if (isSpinner) {
            customSpinner = new CustomAppCompatAutoCompleteTextView(getContext());
            customSpinner.setFocusable(false);
            customSpinner.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            if (((getResources().getConfiguration().screenLayout &
                    Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE)) {
                customSpinner.setPadding(10, 10, 0, 0);
            } else {
                customSpinner.setPadding(30, 45, 0, 0);
            }

            addView(customSpinner);

            if (textSize > 0) {
                customSpinner.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }
            customSpinner.requestLayout();

            customSpinner.setSingleLine();

            customSpinner.setEnabled(enabled);

            if (customSpinner.getAdapter() == null) {
                customSpinner.setOnFocusChangeListener((v, hasFocus) -> {
                });
            }

            customSpinner.setOnClickListener(v -> ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(getApplicationWindowToken(), 0));

            customSpinner.setOnClickListener(v -> customSpinner.showDropDown());

            customSpinner.setGravity(textGravity);
        } else {
            editText = new TextInputEditText(getContext());
            editText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            addView(editText);

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (editText.isEnabled()) {
                        if (s.length() > 0 && !disableClearButton) {
                            setEndIconMode(END_ICON_CLEAR_TEXT);

                            if (endIconDrawable == null && !disableClearButton) {
                                setEndIconDrawable(R.drawable.ic_close);
                            }
                        } else {
                            setEndIconMode(END_ICON_NONE);
                            editText.requestFocus();
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            });

            editText.setEnabled(enabled);

            if (singleLine) {
                editText.setSingleLine();
            }

            if (textSize > 0) {
                editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
            }
            editText.requestLayout();

            if (decimalDigits != 2) {
                String divisors = "1";

                StringBuilder zeros = new StringBuilder();
                for (int i = 0; i < decimalDigits; i++) {
                    zeros.append("0");
                }
                divisors = divisors + zeros;

                divisor = Integer.parseInt(divisors);
            }

            switch (maskType) {
                case 1:
                    //date

                    Drawable startIcon = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_calendar_blank, null);
                    if (startIcon != null) {
                        startIcon.setColorFilter(getResources().getColor(R.color.cinzaEscuro), PorterDuff.Mode.SRC_IN);
                    }

                    if (Build.VERSION.SDK_INT >= 17) {
                        editText.setCompoundDrawablesRelativeWithIntrinsicBounds(startIcon, null, null, null);
                    }

                    editText.setFocusable(false);

                    editText.setOnClickListener(v -> callDialogDate());

                    break;
                case 2:
                    //phone
                    addPhoneMask();
                    break;
                case 3:
                    //brazilDecimal
                    addBrazilDecimalMask();
                    break;
                case 4:
                    //brazilMonetary
                    addBrazilMonetaryMask();
                    break;
                case 5:
                    //brazilCEP
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    setCustomMask("#####-###");
                    break;
                case 6:
                    //brazilCNPJ
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    setCustomMask("##.###.###/####-##");
                    break;
                case 7:
                    //brazilCPF
                    editText.setInputType(InputType.TYPE_CLASS_PHONE);
                    setCustomMask("###.###.###-##");
                    break;
                case 8:
                    //integerDotted
                    decimalDigits = 0;
                    divisor = 1;
                    addBrazilDecimalMask();
                    break;
                default:
                    break;
            }

            if (!specialChars) {
                disableChars();
            }

            if (inputType != EditorInfo.TYPE_TEXT_VARIATION_NORMAL) {
                editText.setInputType(inputType);

                if (inputType == 4097) { //tive de colocar manualmente o código para funcionar pois a constante não pegava
                    InputFilter[] editFilters = editText.getFilters();
                    InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
                    System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
                    newFilters[editFilters.length] = new InputFilter.AllCaps();
                    editText.setFilters(newFilters);
                }
            }

            if (endIconTint != -1) {
                setEndIconTintList(ColorStateList.valueOf(endIconTint));
                setEndIconTintMode(PorterDuff.Mode.SRC_IN);
            }

            editText.setGravity(textGravity);
        }

        if (textColor != null) {
            setTextColorStateList(textColor);
        }

        if (textAlignment >= 0) {
            if (isSpinner) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    customSpinner.setTextAlignment(textAlignment);
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    editText.setTextAlignment(textAlignment);
                }
            }
        }

        if (textStyle >= 0) {
            if (isSpinner) {
                customSpinner.setTypeface(customSpinner.getTypeface(), textStyle);
            } else {
                editText.setTypeface(editText.getTypeface(), textStyle);
            }
        }

        if (maxLength > 0) {
            if (isSpinner) {
                InputFilter[] editFilters = customSpinner.getFilters();
                InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
                System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
                newFilters[editFilters.length] = new InputFilter.LengthFilter(maxLength);
                customSpinner.setFilters(newFilters);
            } else {
                InputFilter[] editFilters = editText.getFilters();
                InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
                System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
                newFilters[editFilters.length] = new InputFilter.LengthFilter(maxLength);
                editText.setFilters(newFilters);
            }
        }

        if (lines > 0) {
            if (isSpinner) {
                customSpinner.setLines(lines);
            } else {
                editText.setLines(lines);
            }
        }

        if (isSpinner) {
            setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        }
    }

    private void addPhoneMask() {
        TextWatcher phoneWatcher = new TextWatcher() {
            private boolean mFormatting; // this is a flag which prevents the stack(onTextChanged)
            private int mLastStartLocation;
            private String mLastBeforeText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (after == 0 && s.toString().equals("(")) {
                    clearFlag = true;
                }
                mLastStartLocation = start;
                mLastBeforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mFormatting) {
                    mFormatting = true;
                    int curPos = mLastStartLocation;
                    String beforeValue = mLastBeforeText;
                    String currentValue = s.toString();
                    String formattedValue = formatPhoneNumber(s);
                    if (currentValue.length() > beforeValue.length()) {
                        int setCusorPos = formattedValue.length()
                                - (beforeValue.length() - curPos);
                        editText.setSelection(setCusorPos < 0 ? 0 : setCusorPos);
                    } else {
                        int setCusorPos = formattedValue.length()
                                - (currentValue.length() - curPos);
                        if (setCusorPos > 0 && !Character.isDigit(formattedValue.charAt(setCusorPos - 1))) {
                            setCusorPos--;
                        }
                        editText.setSelection(setCusorPos < 0 ? 0 : setCusorPos);
                    }
                    mFormatting = false;
                }
            }
        };
        editText.addTextChangedListener(phoneWatcher);
    }

    private void addBrazilDecimalMask() {
        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

        TextWatcher monetaryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.equals("")) {
                    setInitialBrazilDecimal();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);

                String str = s.toString();
                // Retiramos a máscara.
                str = str.replaceAll("[,]", "")
                        .replaceAll("[.]", "").replaceAll("\\s+", "");

                try {
                    NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_BR);
                    nf.setMinimumFractionDigits(decimalDigits);
                    nf.setMaximumFractionDigits(decimalDigits);
                    nf.setGroupingUsed(true);
                    str = nf.format(Double.parseDouble(str) / divisor);
                    setText(str);
                    if (editText.getText() != null) {
                        editText.setSelection(editText.getText().toString().length());
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                editText.addTextChangedListener(this);
            }
        };
        editText.addTextChangedListener(monetaryWatcher);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (mOnFocusChangeListener != null) {
                mOnFocusChangeListener.onFocusChange(v, hasFocus);
            }
            if (!hasFocus) {
                if (getMaskedText().equals("")) {
                    try {
                        setInitialBrazilDecimal();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789,-"));

        setInitialBrazilDecimal();
    }

    private void setInitialBrazilDecimal() {
        NumberFormat nf = NumberFormat.getNumberInstance(LOCALE_BR);
        nf.setMinimumFractionDigits(decimalDigits);
        nf.setMaximumFractionDigits(decimalDigits);
        nf.setGroupingUsed(true);
        String str = nf.format(Double.parseDouble("0") / divisor);
        setText(str);
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().toString().length());
        }
    }

    private void addBrazilMonetaryMask() {
        editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL);

        TextWatcher monetaryWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.equals("")) {
                    setInitialBrazilMonetary();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);

                String str = s.toString();
                // Retiramos a máscara.
                str = str.replaceAll("[R$]", "").replaceAll("[,]", "")
                        .replaceAll("[.]", "").replaceAll("\\s+", "");

                try {
                    NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
                    nf.setMinimumFractionDigits(decimalDigits);
                    nf.setMaximumFractionDigits(decimalDigits);
                    nf.setGroupingUsed(true);
                    str = nf.format(Double.parseDouble(str) / divisor);
                    setText(str);
                    if (editText.getText() != null) {
                        editText.setSelection(editText.getText().toString().length());
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }

                editText.addTextChangedListener(this);
            }
        };
        editText.addTextChangedListener(monetaryWatcher);

        editText.setOnFocusChangeListener((v, hasFocus) -> {
            if (mOnFocusChangeListener != null) {
                mOnFocusChangeListener.onFocusChange(v, hasFocus);
            }
            if (!hasFocus) {
                if (getMaskedText().equals("")) {
                    try {
                        setInitialBrazilMonetary();
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        editText.setKeyListener(DigitsKeyListener.getInstance("0123456789,-"));

        setInitialBrazilMonetary();
    }

    public void setCustomMask(final String mask) {
        //verificando se já existia uma máscara anterior, remove ela
        if (maskWatcher != null) {
            editText.removeTextChangedListener(maskWatcher);
        }

        //criando a nova máscara
        maskWatcher = new TextWatcher() {
            private boolean mFormatting; // this is a flag which prevents the stack(onTextChanged)
            private int mLastStartLocation;
            private String mLastBeforeText;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (after == 0) {
                    clearFlag = true;
                }
                mLastStartLocation = start;
                mLastBeforeText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                //do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!mFormatting) {
                    mFormatting = true;
                    int curPos = mLastStartLocation;
                    String beforeValue = mLastBeforeText;
                    String currentValue = s.toString();

                    String formattedValue;
                    StringBuilder formattedString = new StringBuilder();

                    int digits = mask.length();

                    // Remove everything except digits
                    int p = 0;
                    while (p < s.length()) {
                        char ch = s.charAt(p);
                        if (!Character.isDigit(ch)) {
                            s.delete(p, p + 1);
                        } else {
                            p++;
                        }
                    }

                    // Now only digits are remaining
                    String allDigitString = s.toString();

                    int totalDigitCount = allDigitString.length();

                    if (totalDigitCount > digits) {
                        allDigitString = allDigitString.substring(0, digits);
                        totalDigitCount--;
                    }

                    if (totalDigitCount == 0
                            || totalDigitCount > digits) {
                        // May be the total length of input length is greater than the
                        // expected value so we'll remove all formatting
                        s.clear();
                        s.append(allDigitString);
                        formattedValue = allDigitString;
                    } else {
                        int charMask = 0;
                        int charString = 0;
                        while (charMask < mask.length()) {
                            char chMask = mask.charAt(charMask);

                            if (charString < s.length()) {
                                if (chMask == '#') {
                                    formattedString.append(s.charAt(charString));
                                    charString++;
                                } else {
                                    formattedString.append(chMask);
                                }
                            }
                            charMask++;
                        }

                        s.clear();
                        s.append(formattedString.toString());

                        formattedValue = formattedString.toString();
                    }

                    if (currentValue.length() > beforeValue.length()) {
                        int setCusorPos = formattedValue.length()
                                - (beforeValue.length() - curPos);
                        editText.setSelection(setCusorPos < 0 ? 0 : setCusorPos);
                    } else {
                        int setCusorPos = formattedValue.length()
                                - (currentValue.length() - curPos);
                        if (setCusorPos > 0 && !Character.isDigit(formattedValue.charAt(setCusorPos - 1))) {
                            setCusorPos--;
                        }
                        editText.setSelection(setCusorPos < 0 ? 0 : setCusorPos);
                    }
                    mFormatting = false;
                }
            }
        };
        editText.addTextChangedListener(maskWatcher);
    }

    private void setInitialBrazilMonetary() {
        NumberFormat nf = NumberFormat.getCurrencyInstance(LOCALE_BR);
        nf.setMinimumFractionDigits(decimalDigits);
        nf.setMaximumFractionDigits(decimalDigits);
        nf.setGroupingUsed(true);
        String str = nf.format(Double.parseDouble("0") / divisor);
        setText(str);
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().toString().length());
        }
    }

    private String formatPhoneNumber(Editable text) {
        StringBuilder formattedString = new StringBuilder();

        int digitsPhone = 11;

        // Remove everything except digits
        int p = 0;
        while (p < text.length()) {
            char ch = text.charAt(p);
            if (!Character.isDigit(ch)) {
                text.delete(p, p + 1);
            } else {
                p++;
            }
        }

        // Now only digits are remaining
        String allDigitString = text.toString();

        int totalDigitCount = allDigitString.length();

        if (totalDigitCount > digitsPhone) {
            allDigitString = allDigitString.substring(0, digitsPhone);
            totalDigitCount--;
        }

        // Check if we are dealing with the new phone format, with an additional digit
        boolean isLonger = totalDigitCount == digitsPhone;
        int dashAfter = isLonger ? 5 : 4;

        if (totalDigitCount == 0
                || (totalDigitCount > digitsPhone && !allDigitString.startsWith("("))
                || totalDigitCount > (digitsPhone + 1)) {
            // May be the total length of input length is greater than the
            // expected value so we'll remove all formatting
            text.clear();
            text.append(allDigitString);
            return allDigitString;
        }

        int alreadyPlacedDigitCount = 0;
        // Only ( is remaining and user pressed backspace and so we clear
        // the edit text.
        if (allDigitString.equals("(") && clearFlag) {
            text.clear();
            clearFlag = false;
            return "";
        }

        // The first 2 numbers beyond ) must be enclosed in brackets "()"
        if (totalDigitCount - alreadyPlacedDigitCount > 2) {
            formattedString.append("(").append(allDigitString.substring(alreadyPlacedDigitCount,
                    alreadyPlacedDigitCount + 2)).append(") ");
            alreadyPlacedDigitCount += 2;
        }

        // There must be a '-' inserted after the next 4 or 5 numbers
        // (5 in case we are dealing with the new longer phone format: (xx) xxxxx-xxxx
        if (totalDigitCount - alreadyPlacedDigitCount > dashAfter) {
            formattedString.append(allDigitString.substring(
                    alreadyPlacedDigitCount, alreadyPlacedDigitCount + dashAfter)).append("-");
            alreadyPlacedDigitCount += dashAfter;
        }

        // All the required formatting is done so we'll just copy the
        // remaining digits.
        if (totalDigitCount > alreadyPlacedDigitCount) {
            formattedString.append(allDigitString
                    .substring(alreadyPlacedDigitCount));
        }

        text.clear();
        text.append(formattedString.toString());
        return formattedString.toString();
    }

    private void disableChars() {
        TextWatcher removerAcentosWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                editText.removeTextChangedListener(this);

                String str = s.toString();
                str = Normalizer.normalize(str, Normalizer.Form.NFD)
                        .replaceAll("[^\\p{ASCII}]", "");
                setText(str);
                if (editText.getText() != null) {
                    editText.setSelection(editText.getText().toString().length());
                }

                editText.addTextChangedListener(this);
            }
        };

        editText.addTextChangedListener(removerAcentosWatcher);
    }

    public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
        customSpinner.setAdapter(adapter);
    }

    public void setOnItemSelectedListener(AdapterView.OnItemClickListener listener) {
        customSpinner.setOnItemClickListener(listener);
    }

    private void callDialogDate() {
        if (dataEscolhida == null) {
            dataEscolhida = Calendar.getInstance();
        }

        DatePickerDialog dpd = DatePickerDialog.newInstance(
                (view1, year, monthOfYear, dayOfMonth) -> {
                    dataEscolhida.set(year, (monthOfYear), dayOfMonth);
                    String dataString;
                    if (exibeDiaSemana) {
                        dataString = BRAZIL_DATE_FORMAT.format(
                                dataEscolhida.getTime()) + " - " + new SimpleDateFormat("EEEE", LOCALE_BR)
                                .format(dataEscolhida.getTime());
                    } else {
                        dataString = BRAZIL_DATE_FORMAT.format(dataEscolhida.getTime());
                    }
                    editText.setText(dataString);
                    editText.setError(null);
                },
                dataEscolhida.get(Calendar.YEAR),
                dataEscolhida.get(Calendar.MONTH),
                dataEscolhida.get(Calendar.DAY_OF_MONTH)
        );
        dpd.setCancelText("Limpar");
        dpd.setOnCancelListener(dialogInterface -> {
            editText.setText("");
            dataEscolhida = null;
        });
        dpd.show(((AppCompatActivity) context).getSupportFragmentManager(), "Datepickerdialog");
        dpd.setDateRangeLimiter(new DatePickerRangeLimiter(minDate, maxDate, weekends));
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (isSpinner) {
            customSpinner.setEnabled(enabled);

            if (!enabled) {
                setEndIconMode(TextInputLayout.END_ICON_NONE);
            } else {
                setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
            }
        } else {
            if (editText != null) {
                editText.setEnabled(enabled);

                if (!enabled) {
                    setEndIconMode(END_ICON_NONE);
                }
            }
        }
    }

    public void setTextColor(int color) {
        if (isSpinner) {
            customSpinner.setTextColor(color);
        } else {
            editText.setTextColor(color);
        }
    }

    public void setTextColorStateList(ColorStateList list) {
        if (isSpinner) {
            customSpinner.setTextColor(list);
        } else {
            editText.setTextColor(list);
        }
    }

    public void addTextWatcher(TextWatcher textWatcher) {
        if (!isSpinner) {
            editText.addTextChangedListener(textWatcher);
        } else {
            customSpinner.addTextChangedListener(textWatcher);
        }
    }

    public void removeTextWatcher(TextWatcher textWatcher) {
        if (!isSpinner) {
            editText.removeTextChangedListener(textWatcher);
        } else {
            customSpinner.removeTextChangedListener(textWatcher);
        }
    }

    public void clear() {
        if (isSpinner) {
            customSpinner.setText("");
        } else {
            editText.setText("");
        }
    }

    public String getUnmaskedText() {
        String text;
        if (isSpinner) {
            text = String.valueOf(customSpinner.getText());
            text = text.replaceAll("[./-]+", "").replaceAll(" ", "");
        } else {
            switch (maskType) {
                case 3:
                case 4:
                case 8:
                    //brazilMonetary
                    //brazilDecimal
                    if (editText.getText() == null) {
                        text = "0";
                    } else {
                        if (editText.getText().toString().equals("")) {
                            text = "0";
                        } else {
                            text = editText.getText().toString();
                            text = text.replaceAll("[R$]", "")
                                    .replaceAll("[.]", "")
                                    .replaceAll("\\s+", "");
                            text = text.replaceAll("[,]", ".");
                        }
                    }
                    break;
                default:
                    text = String.valueOf(editText.getText());
                    text = text.replaceAll("[./-]+", "").replaceAll(" ", "");
                    break;
            }
        }

        return text;
    }

    public String getMaskedText() {
        String text;
        if (isSpinner) {
            text = String.valueOf(customSpinner.getText());
        } else {
            text = String.valueOf(editText.getText());
        }

        return text;
    }

    public void setText(String text) {
        if (isSpinner) {
            customSpinner.setText(text);
        } else {
            editText.setText(text);
        }
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        if (isSpinner) {
            customSpinner.setOnEditorActionListener(listener);
        } else {
            editText.setOnEditorActionListener(listener);
        }
    }

    public void myOnFocusChangeListener(View.OnFocusChangeListener listener) {
        if (!isSpinner) {
            editText.setOnFocusChangeListener(listener);
        }
    }

    public void setImeOtions(int imeOtions) {
        if (!isSpinner) {
            editText.setImeOptions(imeOtions);
        }
    }

    @Override
    public void addOnEditTextAttachedListener(@NonNull OnEditTextAttachedListener listener) {
        //super.addOnEditTextAttachedListener(listener);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        if (isSpinner) {
            customSpinner.requestFocus();
        } else {
            editText.requestFocus();
        }
        return true;
    }

    public static Calendar getCalendar(String date) {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy", LOCALE_BR);
        Date data = new Date();
        try {
            data = format.parse(date);
            System.out.println(data);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        final Calendar c = Calendar.getInstance();
        c.setTime(data);

        return c;
    }

    public void setInputType(int inputType) {
        if (!isSpinner) {
            editText.setInputType(inputType);
        }
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        if (!isSpinner) {
            editText.addTextChangedListener(textWatcher);
        }
    }

    public void setSelection(int selection) {
        if (!isSpinner) {
            editText.setSelection(selection);
        }
    }

    public void setOnKeyListener(OnKeyListener onKeyListener) {
        if (!isSpinner) {
            editText.setOnKeyListener(onKeyListener);
        }
    }

    public void setMaxDate(Calendar date) {
        maxDate = date;
    }

    public void setMinDate(Calendar date) {
        minDate = date;
    }

    public void setCustomClickListener(OnClickListener listener) {
        if (isSpinner) {
            customSpinner.setOnClickListener(listener);
        } else {
            editText.setOnClickListener(listener);
        }
    }

    public void setExibeDiaSemana(boolean exibeDiaSemana) {
        this.exibeDiaSemana = exibeDiaSemana;
    }

    public void setWeekends(boolean weekends) {
        this.weekends = weekends;
    }

    public void setDataEscolhida(Calendar dataEscolhida) {
        this.dataEscolhida = dataEscolhida;

        String dataString = "";
        if (dataEscolhida != null) {
            if (exibeDiaSemana) {
                dataString = BRAZIL_DATE_FORMAT.format(
                        dataEscolhida.getTime()) + " - " + new SimpleDateFormat("EEEE", LOCALE_BR)
                        .format(dataEscolhida.getTime());
            } else {
                dataString = BRAZIL_DATE_FORMAT.format(dataEscolhida.getTime());
            }
        }
        editText.setText(dataString);
        editText.setError(null);
    }

    public Calendar getDataEscolhida() {
        return dataEscolhida;
    }

}

class CustomAppCompatAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    private final Context context;

    boolean canPaste() {
        return false;
    }

    @Override
    public boolean isSuggestionsEnabled() {
        return false;
    }

    public CustomAppCompatAutoCompleteTextView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public CustomAppCompatAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public CustomAppCompatAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        this.setCustomSelectionActionModeCallback(new ActionModeCallbackInterceptor());
        this.setLongClickable(false);
    }

    /**
     * Prevents the action bar (top horizontal bar with cut, copy, paste, etc.) from appearing
     * by intercepting the callback that would cause it to be created, and returning false.
     */
    private class ActionModeCallbackInterceptor implements ActionMode.Callback {
        private final String TAG = CustomAppCompatAutoCompleteTextView.class.getSimpleName();

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
        }
    }

}

class DatePickerRangeLimiter
        implements DateRangeLimiter {

    private Calendar startDate;
    private Calendar endDate;
    private boolean weekends = true;

    public DatePickerRangeLimiter(Calendar startDate,
                                  Calendar endDate,
                                  boolean weekends) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.weekends = weekends;
    }

    public DatePickerRangeLimiter(Parcel in) {

    }

    @Override
    public int getMinYear() {
        return 1900;
    }

    @Override
    public int getMaxYear() {
        return 2100;
    }

    @NonNull
    @Override
    public Calendar getStartDate() {
        Calendar output = Calendar.getInstance();
        output.set(Calendar.YEAR, 1900);
        output.set(Calendar.DAY_OF_MONTH, 1);
        output.set(Calendar.MONTH, Calendar.JANUARY);
        return output;
    }

    @NonNull
    @Override
    public Calendar getEndDate() {
        Calendar output = Calendar.getInstance();
        output.set(Calendar.YEAR, 2100);
        output.set(Calendar.DAY_OF_MONTH, 1);
        output.set(Calendar.MONTH, Calendar.JANUARY);
        return output;
    }

    @Override
    public boolean isOutOfRange(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        Calendar now = Calendar.getInstance();

        if (startDate != null && deduzirDatasInt(startDate.getTime(), c.getTime()) < 0) {
            return true;
        } else if (endDate != null && deduzirDatasInt(c.getTime(), endDate.getTime()) < 0) {
            return true;
        } else {
            if (weekends) {
                return false;
            } else {
                int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
                return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
            }
        }
    }

    @NonNull
    @Override
    public Calendar setToNearestDate(@NonNull Calendar day) {
        return day;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

    }

    public static final Parcelable.Creator<DatePickerRangeLimiter> CREATOR
            = new Parcelable.Creator<DatePickerRangeLimiter>() {
        public DatePickerRangeLimiter createFromParcel(Parcel in) {
            return new DatePickerRangeLimiter(in);
        }

        public DatePickerRangeLimiter[] newArray(int size) {
            return new DatePickerRangeLimiter[size];
        }
    };

    public static int deduzirDatasInt(Date initialDate, Date finalDate) {
        if (initialDate == null || finalDate == null) {
            return 0;
        }
        return (int) ((finalDate.getTime() - initialDate.getTime()) / (24 * 60 * 60 * 1000));
    }

}