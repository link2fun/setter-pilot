package com.github.link2fun.setterpilot.completion;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.link2fun.setterpilot.dto.FieldGetterSetter;
import com.github.link2fun.setterpilot.util.PsiCommentUtil;
import com.github.link2fun.setterpilot.util.PsiJavaUtil;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SetterMissAutoCompletion extends CompletionContributor {

    public SetterMissAutoCompletion() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(PsiIdentifier.class), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {

                ApplicationManager.getApplication().executeOnPooledThread(() -> {

                    completionResultSet.restartCompletionOnPrefixChange("set");

                    List<LookupElement> lookupElementList = Lists.newArrayList();

                    ReadAction.run(() -> {
                        lookupElementList.addAll(completionForUnusedSetter(completionParameters, completionResultSet));
                    });

                    if (CollectionUtil.isNotEmpty(lookupElementList)) {
                        lookupElementList.forEach(completionResultSet::addElement);
                    }
//                    ApplicationManager.getApplication().invokeLater(()->{
//
//                    });
                });
            }
        });
    }


//    @Override
//    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
//        completionForUnusedSetter(parameters, result);
//
//    }

    private static List<LookupElement> completionForUnusedSetter(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
//        if (parameters.getCompletionType() != CompletionType.BASIC) {
        System.out.println("completionType not BASIC, current is: " + parameters.getCompletionType());
//            return Lists.newArrayList();
//        }
        System.out.println("1. completionType is BASIC");

        PrefixMatcher prefixMatcher = result.getPrefixMatcher();
        String prefix = prefixMatcher.getPrefix();
        if (!StrUtil.startWithAny(prefix, "set", "se")) {
            System.out.println("prefix not start with set, current is: " + prefix);
            return Lists.newArrayList();
        }
        System.out.println("2. prefix is set");

        PsiElement position = parameters.getPosition();
        if (!(position instanceof PsiIdentifier)) {
            System.out.println("position not instance of PsiIdentifier");
            return Lists.newArrayList();
        }
        System.out.println("3. position is PsiIdentifier");

        PsiElement firstChild = ((PsiReferenceExpressionImpl) ((PsiIdentifierImpl) position).getTreeParent())
                .getFirstChild();
        if (!(firstChild instanceof PsiReferenceExpression)) {
            System.out.println("firstChild not instance of PsiReferenceExpression");
            return Lists.newArrayList();
        }
        System.out.println("4. firstChild is PsiReferenceExpression");
        PsiElement resolve = ((PsiReferenceExpression) firstChild).resolve();
        PsiType type;

        if (resolve instanceof PsiLocalVariable psiLocalVariable) {

            type = psiLocalVariable.getType();
        } else if (resolve instanceof PsiParameter) {
            type = ((PsiParameter) resolve).getType();
        } else {
            System.out.println("resolve not instance of PsiLocalVariable or PsiParameter");
            return Lists.newArrayList();
        }
        System.out.println("5. resolve is PsiLocalVariable");
        // 找到对应类型的 setter 方法

        Project project = position.getProject();
        PsiClass aClass = PsiJavaUtil.getPsiClass(project, type.getCanonicalText());
        List<FieldGetterSetter> typeSetterMethodList = PsiJavaUtil.getFieldsWithGetterAndSetter(aClass);
        System.out.println("6. typeSetterMethodList is " + typeSetterMethodList);

        Collection<PsiReference> callList = ReferencesSearch.search(resolve).findAll();
        System.out.println("7. callList is " + callList);
        // 找到里面所有的 methodCall
        List<PsiMethodCallExpression> methodCallExpressionList = callList.stream()
                .map(ele -> ele.getElement().getParent().getParent())
                .filter(ele -> ele instanceof PsiMethodCallExpression)
                .map(ele -> (PsiMethodCallExpression) ele).toList();
        System.out.println("8. methodCallExpressionList is " + methodCallExpressionList);

        // 设置已经调用的方法

        // 从中找到已经调用的setter 方法
        List<String> setterListCalled = methodCallExpressionList.stream()
                .map(methodCall -> methodCall.getMethodExpression().getLastChild().getText())
                .toList();
        System.out.println("9. setterListCalled is " + setterListCalled);

        // 找到尚未调用的方法
        List<FieldGetterSetter> setterListMiss = typeSetterMethodList.stream()
                .filter(info -> Objects.nonNull(info.getSetter()))
                .filter(setter -> !setterListCalled.contains(setter.getSetter().getName()))
                .collect(Collectors.toList());
        System.out.println("10. setterListMiss is " + setterListMiss);

        if (CollectionUtil.isEmpty(setterListMiss)) {
            System.out.println("没有找到setter方法");
            return Lists.newArrayList();
        }
        System.out.println("11. setterListMiss is not empty");


        List<LookupElement> lookupElementList = Lists.newArrayList();

        for (FieldGetterSetter fieldWithGetterSetter : setterListMiss) {
            String setterMethodName = fieldWithGetterSetter.getSetter().getName();
            System.out.println("===========> add " + setterMethodName);
            PsiDocComment docComment = fieldWithGetterSetter.getField().getDocComment();

            String commentStr = PsiCommentUtil.getCommentFirstLine(docComment);


            LookupElement lookupElementNormalSetter = PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("setter " + setterMethodName + "()" + (StrUtil.isNotBlank(commentStr) ? " // " + commentStr : ""))
                            .withTypeText("尚未调用")
                            .withInsertHandler((context, item) -> {

                                String content = setterMethodName + "();"
                                        + (StrUtil.isNotBlank(commentStr) ? " // " + commentStr : "");
                                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(),
                                        content);
                                // 移动光标到括号中间
                                context.getEditor().getCaretModel()
                                        .moveToOffset(context.getStartOffset() + setterMethodName.length() + 1);
                            }),
                    Integer.MAX_VALUE);

            lookupElementList.add(lookupElementNormalSetter);
        }
        return lookupElementList;
    }
}
