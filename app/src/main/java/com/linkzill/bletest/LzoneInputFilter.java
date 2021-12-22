package com.linkzill.bletest;

import android.text.InputFilter;
import android.text.Spanned;

/**
 * Author:Think
 * Time:2021/5/3 0:59
 * Description:This is LzoneInputFilter
 */
public class LzoneInputFilter implements InputFilter {

    private String mReg;

    public LzoneInputFilter(String reg) {
        this.mReg = reg;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

        StringBuffer sb = new StringBuffer();

        for(int i=0;i<source.length();i++){
            if(mReg.indexOf(source.charAt(i)) >= 0){
                sb.append(source.charAt(i));
            }
        }

        return sb;
    }
}
