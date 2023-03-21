package br.com.customtextinputlayout;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.CompositeDateValidator;
import com.google.android.material.datepicker.DateValidatorPointBackward;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.text.DateFormat;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    private final Context context;

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
            if (attr == R.styleable.CustomTextInputLayout_android_textSize) {
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

                        editText.requestFocus();
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
            case 9:
                //date no icon
                editText.setFocusable(false);
                editText.setOnClickListener(v -> callDialogDate());
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

        if (textColor != null) {
            setTextColorStateList(textColor);
        }

        if (textAlignment >= 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                editText.setTextAlignment(textAlignment);
            }
        }

        if (textStyle >= 0) {
            editText.setTypeface(editText.getTypeface(), textStyle);
        }

        if (maxLength > 0) {
            InputFilter[] editFilters = editText.getFilters();
            InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
            System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
            newFilters[editFilters.length] = new InputFilter.LengthFilter(maxLength);
            editText.setFilters(newFilters);
        }

        if (lines > 0) {
            editText.setLines(lines);
        }
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);

        if (gainFocus) {
            if (editText.isEnabled() && editText != null && editText.getText() != null) {
                if (editText.getText().toString().length() > 0 && !disableClearButton) {
                    setEndIconMode(END_ICON_CLEAR_TEXT);

                    if (endIconDrawable == null && !disableClearButton) {
                        setEndIconDrawable(R.drawable.ic_close);
                    }

                    //editText.requestFocus();
                } else {
                    setEndIconMode(END_ICON_NONE);
                    //editText.requestFocus();
                }
            }
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

    private void callDialogDate() {
        if (dataEscolhida == null) {
            dataEscolhida = Calendar.getInstance();
        }

        CalendarConstraints.Builder constraintsBuilderRange = new CalendarConstraints.Builder();

        ArrayList<CalendarConstraints.DateValidator> listValidators = new ArrayList<>();

        if (minDate != null) {
            CalendarConstraints.DateValidator dateValidatorMin = DateValidatorPointForward.from(minDate.getTimeInMillis());
            listValidators.add(dateValidatorMin);
        }

        if (maxDate != null) {
            CalendarConstraints.DateValidator dateValidatorMax = DateValidatorPointBackward.before(maxDate.getTimeInMillis());
            listValidators.add(dateValidatorMax);
        }

        if (!weekends) {
            WeekdayDateValidator weekdayDateValidator = new WeekdayDateValidator();
            listValidators.add(weekdayDateValidator);
        }

        CalendarConstraints.DateValidator validators = CompositeDateValidator.allOf(listValidators);
        constraintsBuilderRange.setValidator(validators);

        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
                .setSelection(dataEscolhida.getTimeInMillis())
                .setNegativeButtonText("LIMPAR")
                .setCalendarConstraints(constraintsBuilderRange.build())
                .build();

        dp.addOnPositiveButtonClickListener(selection -> {
            Calendar utc = Calendar.getInstance();
            utc.setTimeInMillis(selection);
            utc.set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH) + 1);
            dataEscolhida = utc;

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
        });
        dp.addOnNegativeButtonClickListener(v -> {
            editText.setText("");
            dataEscolhida = null;
        });
        dp.show(((AppCompatActivity) context).getSupportFragmentManager(), "Datepickerdialog");
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (editText != null) {
            editText.setEnabled(enabled);

            if (!enabled) {
                setEndIconMode(END_ICON_NONE);
            }
        }
    }

    public void setTextColor(int color) {
        editText.setTextColor(color);
    }

    public void setTextColorStateList(ColorStateList list) {
        editText.setTextColor(list);
    }

    public void addTextWatcher(TextWatcher textWatcher) {
        editText.addTextChangedListener(textWatcher);
    }

    public void removeTextWatcher(TextWatcher textWatcher) {
        editText.removeTextChangedListener(textWatcher);
    }

    public void clear() {
        editText.setText("");
    }

    public String getUnmaskedText() {
        String text;
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

        return text;
    }

    public String getMaskedText() {
        String text;
        text = String.valueOf(editText.getText());
        return text;
    }

    public void setText(String text) {
        editText.setText(text);
    }

    public void setOnEditorActionListener(TextView.OnEditorActionListener listener) {
        editText.setOnEditorActionListener(listener);
    }

    public void myOnFocusChangeListener(View.OnFocusChangeListener listener) {
        editText.setOnFocusChangeListener(listener);
    }

    public void setImeOtions(int imeOtions) {
        editText.setImeOptions(imeOtions);
    }

    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        editText.requestFocus();
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
        editText.setInputType(inputType);
    }

    public void addTextChangedListener(TextWatcher textWatcher) {
        editText.addTextChangedListener(textWatcher);
    }

    public void setSelection(int selection) {
        editText.setSelection(selection);
    }

    public void setOnKeyListener(OnKeyListener onKeyListener) {
        editText.setOnKeyListener(onKeyListener);
    }

    public void setMaxDate(Calendar date) {
        maxDate = date;
    }

    public void setMinDate(Calendar date) {
        minDate = date;
        Log.i("Teste", "minDate = " + minDate);
    }

    public void setCustomClickListener(OnClickListener listener) {
        editText.setOnClickListener(listener);
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

    public void setFilters(InputFilter[] filters) {
        editText.setFilters(filters);
    }

    public void setFocusable(boolean focusable) {
        editText.setFocusable(focusable);
    }

    public void setTextSize(float size) {
        editText.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setOnTouchListener(View.OnTouchListener listener) {
        editText.setOnTouchListener(listener);
    }

}

class WeekdayDateValidator implements CalendarConstraints.DateValidator {

    WeekdayDateValidator() {

    }

    WeekdayDateValidator(Parcel parcel) {

    }

    @Override
    public boolean isValid(long date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(date);
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        return dayOfWeek != Calendar.FRIDAY && dayOfWeek != Calendar.SATURDAY;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {

    }

    public static final Parcelable.Creator<WeekdayDateValidator> CREATOR = new Parcelable.Creator<WeekdayDateValidator>() {

        @Override
        public WeekdayDateValidator createFromParcel(Parcel parcel) {
            return new WeekdayDateValidator(parcel);
        }

        @Override
        public WeekdayDateValidator[] newArray(int size) {
            return new WeekdayDateValidator[size];
        }
    };

}