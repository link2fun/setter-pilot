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

    // 获取原始类（从源码）
    PsiClass originalClass = (PsiClass) psiClass.getNavigationElement();
    logger.debug("获取类: {}, 是否为源码类: {}", originalClass.getQualifiedName(), originalClass != psiClass);

    // 获取所有的字段（从源码）
    PsiField[] psiFields = originalClass.getAllFields();
    logger.debug("获取到字段数量: {}", psiFields.length);

    // 获取所有方法（从源码）
    PsiMethod[] psiMethods = originalClass.getAllMethods();
    logger.debug("获取到方法数量: {}", psiMethods.length);

    List<FieldGetterSetter> result = Lists.newArrayList();

    // 开始匹配
    for (PsiField psiField : psiFields) {
      // 获取原始字段（从源码）
      PsiField originalField = (PsiField) psiField.getNavigationElement();
      logger.debug("处理字段: {}, 是否有注释: {}", originalField.getName(), originalField.getDocComment() != null);

      PsiMethod getter = null;
      PsiMethod setter = null;
      for (PsiMethod psiMethod : psiMethods) {
        String methodName = psiMethod.getName();
        // 检查是否是当前字段的 getter/setter
        if (!isGetterOrSetterForField(methodName, originalField.getName())) {
          continue;
        }

        // getter , 如果没有方法参数, 且有返回值, 则认定是 getter
        if (methodName.startsWith("get") && psiMethod.getParameterList().isEmpty() 
            && psiMethod.getReturnType() != null 
            && StrUtil.equals(psiMethod.getReturnType().getCanonicalText(), originalField.getType().getCanonicalText())) {
          getter = psiMethod;
          continue;
        }

        // setter , 如果只有一个参数, 且参数类型匹配, 则认定是 setter
        if (methodName.startsWith("set") && psiMethod.getParameterList().getParametersCount() == 1 
            && psiMethod.getParameterList().getParameters()[0].getType().getCanonicalText().equals(originalField.getType().getCanonicalText())) {
          setter = psiMethod;
        }
      }

      result.add(FieldGetterSetter.of(originalField, getter, setter));
    }
    return result;
  }

  /**
   * 检查方法名是否是指定字段的 getter 或 setter
   */
  private static boolean isGetterOrSetterForField(String methodName, String fieldName) {
    if (fieldName == null || methodName == null) {
      return false;
    }
    String capitalizedFieldName = StrUtil.upperFirst(fieldName);
    return methodName.equals("get" + capitalizedFieldName) 
        || methodName.equals("set" + capitalizedFieldName)
        || methodName.equals("is" + capitalizedFieldName);  // 处理 boolean 类型的 getter
  }
}
