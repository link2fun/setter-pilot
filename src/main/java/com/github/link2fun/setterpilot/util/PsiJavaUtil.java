package com.github.link2fun.setterpilot.util;

import cn.hutool.core.util.StrUtil;
import com.github.link2fun.setterpilot.dto.FieldGetterSetter;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * 一些不便分类的Java Psi 工具类
 *
 * @author link2fun
 */
public class PsiJavaUtil {

  private static final Logger logger = LoggerFactory.getLogger(PsiJavaUtil.class);


  /**
   * 全局找到指定全限定名的类
   *
   * @param project       项目
   * @param qualifiedName 类全限定名
   * @return {@code PsiClass}
   */
  public static PsiClass getPsiClass(Project project, String qualifiedName) {
    // 使用更大的搜索范围，包含项目和所有依赖
    GlobalSearchScope scope = GlobalSearchScope.everythingScope(project);
    PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, scope);
    if (psiClass == null) {
      logger.warn("无法找到类: {}", qualifiedName);
    } else {
      logger.debug("成功找到类: {}", qualifiedName);
    }
    return psiClass;
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
        PsiElement navigationElement = psiMethod.getNavigationElement();
        if (navigationElement == null) {
          // 没有关联的元素, 应该不是 getter or setter
          continue;
        }
        if (!(navigationElement instanceof PsiField)) {
          continue;
        }

        PsiField navField = (PsiField) navigationElement;

        if (!StrUtil.equals(navField.getName(), psiField.getName())) {
          // 不是当前字段
          continue;
        }

        if (getter != null && setter != null) {
          continue;
        }


        // getter , 如果没有方法参数, 且有返回值, 则初步认定是 getter
        if (psiMethod.getParameterList().isEmpty() && StrUtil.equals(psiMethod.getReturnType().getCanonicalText(),psiField.getType().getCanonicalText()) ) {
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
