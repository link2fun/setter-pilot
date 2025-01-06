package com.github.link2fun.setterpilot.util;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.intellij.psi.JavaDocTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;

import java.util.List;

/**
 * 文档注释工具类
 */
public class PsiCommentUtil {

    /**
     * 获取文档注释的文本内容, 并拼接成字符串
     *
     * @param comment   文档注释
     * @param separator 分隔符
     * @return 文本内容
     */
    public static String getCommentStr(PsiDocComment comment, String separator) {
        return StrUtil.join(separator, getCommentDataList(comment));
    }


    /**
     * 获取文档注释第一行内容
     *
     * @param comment 文档注释
     * @return 第一行内容
     */
    public static String getCommentFirstLine(PsiDocComment comment) {

        return CollectionUtil.getFirst(getCommentDataList(comment));

    }


    /**
     * 获取文档注释的纯文本内容
     */
    public static List<String> getCommentDataList(PsiDocComment comment) {
        if (comment == null) {
            return null;
        }

        PsiElement[] commentChildren = comment.getChildren();

        List<String> commentDataList = Lists.newArrayList();

        for (PsiElement commentChild : commentChildren) {
            if (!(commentChild instanceof PsiDocToken)) {
                // 如果不是 PsiDocToken 类型, 跳过
                continue;
            }

            PsiDocToken docToken = (PsiDocToken) commentChild;

            if (docToken.getTokenType() == JavaDocTokenType.DOC_COMMENT_DATA) {
                String tokenText = docToken.getText();
                if (StrUtil.isNotBlank(tokenText)) {
                    commentDataList.add(tokenText);
                }
            }
        }

        return commentDataList;

    }

}
