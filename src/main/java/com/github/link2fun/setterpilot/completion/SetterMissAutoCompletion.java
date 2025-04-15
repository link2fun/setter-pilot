package com.github.link2fun.setterpilot.completion;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.github.link2fun.setterpilot.dto.FieldGetterSetter;
import com.github.link2fun.setterpilot.util.PsiCommentUtil;
import com.github.link2fun.setterpilot.util.PsiJavaUtil;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.actions.ReformatCodeProcessor;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SetterMissAutoCompletion extends CompletionContributor {

  private static final Logger logger = Logger.getInstance(SetterMissAutoCompletion.class);


  public SetterMissAutoCompletion() {
    extend(CompletionType.BASIC, PlatformPatterns.psiElement(PsiIdentifier.class), new CompletionProvider<CompletionParameters>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
        // 在开始时重启completion
        completionResultSet.restartCompletionOnPrefixChange("set");

        // 在 ReadAction 中执行所有操作
        List<LookupElement> lookupElementList = ReadAction.compute(() -> {
          return completionForUnusedSetter(completionParameters, completionResultSet);
        });

        // 添加结果
        if (CollectionUtil.isNotEmpty(lookupElementList)) {
          lookupElementList.forEach(completionResultSet::addElement);
        }
      }
    });
  }


  private static List<LookupElement> completionForUnusedSetter(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    logger.debug("completionType not BASIC, current is: {}", parameters.getCompletionType());
    logger.debug("1. completionType is BASIC");

    PrefixMatcher prefixMatcher = result.getPrefixMatcher();
    String prefix = prefixMatcher.getPrefix();
    if (!StrUtil.startWithAny(prefix, "set", "se")) {
      logger.debug("prefix not start with set, current is: {}", prefix);
      return Lists.newArrayList();
    }
    logger.debug("2. prefix is set");

    PsiElement position = parameters.getPosition();
    if (!(position instanceof PsiIdentifier)) {
      logger.debug("position not instance of PsiIdentifier");
      return Lists.newArrayList();
    }
    logger.debug("3. position is PsiIdentifier");

    PsiElement firstChild = ((PsiReferenceExpressionImpl) ((PsiIdentifierImpl) position).getTreeParent())
      .getFirstChild();
    if (!(firstChild instanceof PsiReferenceExpression)) {
      logger.debug("firstChild not instance of PsiReferenceExpression");
      return Lists.newArrayList();
    }
    logger.debug("4. firstChild is PsiReferenceExpression");
    PsiElement resolve = ((PsiReferenceExpression) firstChild).resolve();
    PsiType type;

    if (resolve instanceof PsiLocalVariable psiLocalVariable) {

      type = psiLocalVariable.getType();
    } else if (resolve instanceof PsiParameter) {
      type = ((PsiParameter) resolve).getType();
    } else {
      logger.debug("resolve not instance of PsiLocalVariable or PsiParameter");
      return Lists.newArrayList();
    }
    logger.debug("5. resolve is PsiLocalVariable");
    // 找到对应类型的 setter 方法

    Project project = position.getProject();
    PsiClass aClass = PsiJavaUtil.getPsiClass(project, type.getCanonicalText());
    List<FieldGetterSetter> typeSetterMethodList = PsiJavaUtil.getFieldsWithGetterAndSetter(aClass);
    logger.debug("6. typeSetterMethodList is {}", typeSetterMethodList);

    Collection<PsiReference> callList = ReferencesSearch.search(resolve).findAll();
    logger.debug("7. callList is {}", callList);
    // 找到里面所有的 methodCall
    List<PsiMethodCallExpression> methodCallExpressionList = callList.stream()
      .map(ele -> ele.getElement().getParent().getParent())
      .filter(ele -> ele instanceof PsiMethodCallExpression)
      .map(ele -> (PsiMethodCallExpression) ele).toList();
    logger.debug("8. methodCallExpressionList is {}", methodCallExpressionList);

    // 设置已经调用的方法

    // 从中找到已经调用的setter 方法
    List<String> setterListCalled = methodCallExpressionList.stream()
      .map(methodCall -> methodCall.getMethodExpression().getLastChild().getText())
      .toList();
    logger.debug("9. setterListCalled is {}", setterListCalled);

    // 找到尚未调用的方法
    List<FieldGetterSetter> setterListMiss = typeSetterMethodList.stream()
      .filter(info -> Objects.nonNull(info.getSetter()))
      .filter(info -> info.getField().getAnnotations().length == 0 || 
                     Arrays.stream(info.getField().getAnnotations())
                           .noneMatch(annotation -> "com.easy.query.core.annotation.Navigate"
                                    .equals(annotation.getQualifiedName())))
      .filter(setter -> !setterListCalled.contains(setter.getSetter().getName()))
      .collect(Collectors.toList());
    logger.debug("10. setterListMiss is {}", setterListMiss);

    if (CollectionUtil.isEmpty(setterListMiss)) {
      logger.debug("没有找到setter方法");
      return Lists.newArrayList();
    }
    logger.debug("11. setterListMiss is not empty");


    List<LookupElement> lookupElementList = Lists.newArrayList();

    for (FieldGetterSetter fieldWithGetterSetter : setterListMiss) {
      String setterMethodName = fieldWithGetterSetter.getSetter().getName();
      logger.debug("===========> add {}", setterMethodName);
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

    // Add ALL_UNUSED setter
    if (!setterListMiss.isEmpty()) {
      LookupElement lookupElementAllUnusedSetter = PrioritizedLookupElement.withPriority(
        LookupElementBuilder.create("setter ALL_UNUSED")
          .withTypeText("尚未调用")
          .withInsertHandler((context, item) -> {

            PsiExpressionStatement expressionStatement = PsiTreeUtil.getParentOfType(position, PsiExpressionStatement.class);

            PsiIdentifier identifier = PsiTreeUtil.findChildOfType(expressionStatement, PsiIdentifier.class);
            if (identifier == null) {
              // 这个 identifier 是 null，不应该存在
              logger.error("identifier is null");
              return;
            }

            int startOffset = identifier.getTextOffset();

            StringBuilder content = new StringBuilder();
            for (FieldGetterSetter fieldWithGetterSetter : setterListMiss) {
              String setterMethodName = fieldWithGetterSetter.getSetter().getName();
              String commentStr = PsiCommentUtil.getCommentFirstLine(fieldWithGetterSetter.getField().getDocComment());
              String varName = identifier.getText();
              content
                .append(varName).append(".")
                .append(setterMethodName).append("();")
                .append(StrUtil.isNotBlank(commentStr) ? " // " + commentStr : "")
                .append("\n");
            }

            context.getDocument().replaceString(startOffset, context.getTailOffset(), content.toString());

            // 新增后的代码结束位置
            int endOffset = startOffset + content.length();

            // 格式化代码
            ApplicationManager.getApplication().runWriteAction(() -> {

              Editor editor = context.getEditor();

              PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());

              PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
              if (file == null) return;

              // 格式化 插入的代码
              ReformatCodeProcessor reformatCodeProcessor = new ReformatCodeProcessor(project, file, new TextRange(startOffset, endOffset), false);
              reformatCodeProcessor.run();


            });
          }),
        Integer.MAX_VALUE);

      lookupElementList.add(lookupElementAllUnusedSetter);
    }


    return lookupElementList;
  }
}
