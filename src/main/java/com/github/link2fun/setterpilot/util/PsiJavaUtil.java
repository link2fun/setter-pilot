package com.github.link2fun.setterpilot.util;

import com.github.link2fun.setterpilot.dto.FieldGetterSetter;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.List;


/**
 * 一些不便分类的Java Psi 工具类
 *
 * @author link2fun
 */
public class PsiJavaUtil {


    /**
     * 全局找到指定全限定名的类
     *
     * @param project       项目
     * @param qualifiedName 类全限定名
     * @return {@code PsiClass}
     */
    public static PsiClass getPsiClass(Project project, String qualifiedName) {
        return JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.allScope(project));
    }


    /**
     * 找到指定 PsiClass 下的字段 及其对应的 getter 和 setter
     *
     * @param psiClass PsiClass
     * @return Tuple3<字段, getter, setter>
     */
    public static List<FieldGetterSetter> getFieldsWithGetterAndSetter(PsiClass psiClass) {
        if (psiClass == null) {
            return Lists.newArrayList();
        }


        // 获取所有的字段
        PsiField[] psiFields = psiClass.getAllFields();

        // 获取所有方法
        PsiMethod[] psiMethods = psiClass.getAllMethods();

        List<FieldGetterSetter> result = Lists.newArrayList();

        // 开始匹配
        for (PsiField psiField : psiFields) {

            PsiMethod getter = null;
            PsiMethod setter = null;
            for (PsiMethod psiMethod : psiMethods) {
                if (psiMethod.getNavigationElement() == null) {
                    // 没有关联的元素, 应该不是 getter or setter
                    continue;
                }

                if (psiMethod.getNavigationElement() != psiField) {
                    // 不是当前字段
                    continue;
                }

                if (getter != null && setter != null) {
                    continue;
                }


                // getter , 如果没有方法参数, 且有返回值, 则初步认定是 getter
                if (psiMethod.getParameterList().isEmpty() && psiMethod.getReturnType() == psiField.getType()) {
                    getter = psiMethod;
                    continue;
                }

                // setter , 如果只有一个参数, 且返回值是 void, 则初步认定是 setter
                if (psiMethod.getParameterList().getParametersCount() == 1) {
                    setter = psiMethod;
                    continue;
                }

            }


            result.add(FieldGetterSetter.of(psiField, getter, setter));


        }
        return result;
    }
}
