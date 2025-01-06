package com.github.link2fun.setterpilot.dto;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FieldGetterSetter {

    private PsiField field;

    private PsiMethod getter;

    private PsiMethod setter;

    public static FieldGetterSetter of(PsiField field, PsiMethod getter, PsiMethod setter) {
        FieldGetterSetter fieldGetterSetter = new FieldGetterSetter();
        fieldGetterSetter.setField(field);
        fieldGetterSetter.setGetter(getter);
        fieldGetterSetter.setSetter(setter);
        return fieldGetterSetter;
    }

}
